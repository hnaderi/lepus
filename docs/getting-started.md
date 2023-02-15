# Getting started

This library is published for scala 3 on all scala platforms (JVM, JS, Native); 
Add this to your build:

```scala
libraryDependencies += "dev.hnaderi" %% "lepus-client" % "@VERSION@"
```

Then you are ready to use Lepus!  
If you are a beginner in AMQP (RabbitMQ) or messaging in general please take a look at [here](https://www.rabbitmq.com/getstarted.html) before continuing.

## Imports

You need to import these:
```scala mdoc:silent
import cats.effect.IO
import com.comcast.ip4s.* // 1

import lepus.client.* // 2
import lepus.protocol.domains.*  // 3
```

1. for string literals `host` and `port`
2. import client stuff
3. import AMQP data models

## Opening a connection

```scala mdoc:silent
val connection = LepusClient[IO]( // 1
   host = host"localhost",
   port = port"5672",
   username = "guest",
   password = "guest",
   vhost = Path("/"), // 2
   config = ConnectionConfig.default,
   debug = false
)
```

1. Note that all of the parameters are optional and have the default values as shown in the example above.  
2. Path is a data model defined in AMQP protocol that is usued for virtual hosts

This gives us a `Resource` that opens a connection.

## Opening a channel

Having opened a connection, we can open channels on it:

```scala mdoc:silent
val channel = for {
  con <- connection
  ch <- con.channel
} yield ch
```

which gives us a resource that opens a new channel on `connection`

### Commands

Having a channel in hand, we can call AMQP methods

```scala mdoc:silent
val app1 = channel.use(ch=>
  ch.exchange.declare(ExchangeName("events"), ExchangeType.Topic) >>
  ch.queue.declare(QueueName("handler-inbox")) >>
  ch.queue.bind(QueueName("handler-inbox"), ExchangeName("events"), ShortString("#"))
)
```

### Publish
We can also publish messages:

```scala mdoc:silent
val publisher1 = channel.use(ch=>
  ch.messaging.publish(
    ExchangeName("notifications"), 
    routingKey = ShortString("some.topic"),
    Message("Something happend!")
  )
)
```

We can also publish mandatory messages like this:

```scala mdoc:silent
import fs2.Stream

val toPublish = Stream(
  Envelope(
    ExchangeName("notifications"), 
    routingKey = ShortString("some.topic"),
    mandatory  = true,
    Message("Something happend!")
  )
)

val publisher2 = Stream.resource(channel).flatMap(ch=>
  toPublish.through(ch.messaging.publisher) // 1
)
```

1. @:api(lepus.client.apis.NormalMessagingChannel.publisher) is a pipe that publishes envelopes, and outputs returned messages if any.

### Consumer

```scala mdoc:silent
val consumer1 = Stream
  .resource(channel)
  .flatMap(_.messaging.consume[String](
       QueueName("jobs"), 
       mode = ConsumeMode.NackOnError // 1
     )
   )
```

1. @:api(lepus.client.ConsumeMode) determines how a decoding consumer behaves, `ConsumeMode.RaiseOnError` raises error when decoding is failed, `ConsumeMode.NackOnError` sends a nack for the failed message. `RaiseOnError(false)` consumes in auto ack mode, the other two requires you to acknowledge each message

## More advanced channels
so far we've been using @:api(lepus.client.apis.NormalMessagingChannel), which does not support transactions, or publisher confirmation.  

Let's see how we can use those features:

### transactional channels

We can open a transactional channel like this:

```scala mdoc:silent
import cats.effect.Resource

val transactional1 = for {
  con <- connection
  ch <- con.transactionalChannel

  trx <- ch.messaging.transaction // start a new transaction boundary
  // we are inside a transaction now
  
  _ <- Resource.eval(ch.messaging.publish(ExchangeName("events"), ShortString("topic"), "some data!"))
  
  _ <- Resource.eval(trx.rollback) // or commit
} yield ()
```

### confirming channels

For using publisher confirms, we need to open a channel like this:

```scala mdoc:silent
val confirming1 = for {
  con <- Stream.resource(connection)
  ch <- Stream.resource(con.reliableChannel)
  
  // publishing gives us a delivery tag
  dTags = Stream.eval(ch.messaging.publish(ExchangeName("events"), ShortString("topic"), "some data!"))
  
  // confirmations gives us server acknowledgements based on the delivery tags
  confirmations = ch.messaging.confirmations
  
  // run in parallel and handle responses
  // to ensure you have published a message reliably
  _ <- dTags.mergeHaltBoth(confirmations).foreach(???)
} yield ()
```

## More advanced publishing
There are some other more advanced publishing methods, like publishing mandatory messages in publisher confirming mode, which are out of the scope of getting started :)  
However you can take a look at api docs and always feel free to read codes or open issues and PRs.
