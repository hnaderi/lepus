# Examples

All of this examples are also hosted on https://lepus.hnaderi.dev/examples

## Environment
You need to have default rabbitmq up and running on localhost:5672 before running this examples;
You can use this command to fire up one:

``` sh
docker run --rm -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

## Hello world

Run this example using:
``` sh
scala-cli https://lepus.hnaderi.dev/examples/HelloWorld.scala
```

Or take a look at its [source code](HelloWorld.scala)

## WorkPool

This example uses [stdlib](/standard-library.md) to implement a work pool topology.

Run the server part of this example using:
``` sh
scala-cli https://lepus.hnaderi.dev/examples/WorkPool.scala -- server
```
It will read stdin and send every line as a task to the work pool.

Run any number of workers using:
``` sh
scala-cli https://lepus.hnaderi.dev/examples/WorkPool.scala -- worker worker1
```

Or take a look at its [source code](WorkPool.scala)

## PubSub

This example uses [stdlib](/standard-library.md) to implement a pub/sub topology.

Run this example using:
``` sh
scala-cli https://lepus.hnaderi.dev/examples/PubSub.scala
```

Or take a look at its [source code](PubSub.scala)
