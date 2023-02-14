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
val app2 = channel.use(ch=>
  ch.messaging.publish(
    ExchangeName("notifications"), 
    routingKey = ShortString("some.topic"),
    Message("Something happend!")
  )
)
```

### Consumer

```scala mdoc:silent
import fs2.Stream

val app3 = Stream
  .resource(channel)
  .flatMap(_.messaging.consume[String](QueueName("jobs"), mode = ConsumeMode.NackOnError))
```

## More advanced channels

### transactional channels

### confirming channels

## More advanced publishing
