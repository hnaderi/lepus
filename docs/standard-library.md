# Standard library

Although AMQP supports a vast amount of different messaging topologies, not all of its users want to 
write everything from the scratch every time! also most of the use cases that are common in practice, 
don't even require that much flexibility.
Besides, having some opinionated tools that follow best practices help beginners and facilitates advanced users.

This is where Lepus standard library comes in to play, and provides out of the box utilities and helpers for the most common messaging use cases,
and imposes a few best practices and design opinions.

in order to use standard library, you need to add its dependency:

```scala
libraryDependencies += "dev.hnaderi" %% "lepus-std" % "@VERSION@"
```

and import its package:

```scala mdoc
import lepus.std.*
```

## WorkPoolChannel
@:api(lepus.std.WorkPoolChannel) implements a work pool topology. In this topology, 
one or more peers produce jobs, and one or more worker compete over processing those jobs.
This topology handles workers fail over, so if a worker fails, its jobs will be routed to another worker.
However this topology can't guarantee any ordering of messages by definition.


TBD code example

## RPCChannel

@:api(lepus.std.RPCChannel) implements an async RPC communication channel topology.
In this topology, each server has its own endpoint, where clients can send methods to,
server then can decide to response to sender's address, ignore the request, or reject it.
Clients can then consume responses, and mark them as processed.
This topology models an point to point communication, with at least one delivery semantics, 
so your processing MUST be idempotent and async, as both responses and requests might be received several times,
and with any ordering.

TBD code example

## EventChannel

@:api(lepus.std.EventChannel) implements a pubsub topology for events.
In this topology, peers publish or subscribe to certain communication channels (logical streams of data).
In this topology every consumer gets a copy of data, which is in contrast to previous topologies where a single piece 
of data is routed to exactly one peer.
This topology guarantees at least one delivery of messages.

TBD code example

## Helpers

### ShortString constructors
AMQP protocol defines a set of data models, `ShortString` is one of them that is frequently used in APIs and messages,
for example, routing keys and most of the message properties are all `ShortString`s.  
Generally speaking, `ShortString` is any UTF-8 string with maximum length of 255 (however there are some other cases where there are more constraints).  
Being such a common data type, it can be very helpful to have constructors for common use cases that are safe at compile time.
The following syntax is provided by stdlib:

```scala mdoc
val example1 = "some id".md5Hex
val example2 = "some id".sha1Hex
// sha1 sha224 sha256 sha384 and sha512 are supported
val example3 = "some id".sha512Hex

//This can also be accessed like this
import lepus.protocol.domains.*

val example4 = ShortString.md5Hex("some id")
```
