# Features

## AMQP 0.9.1 compliant
Supports all AMQP 0.9.1 protocol, from transactions to pluggable authentication mechanism.

## RabbitMQ protocol extensions
Supports almost all of the RabbitMQ protocol extensions
A couple of extensions are not implemented yet which is tracked in [this issue](https://github.com/hnaderi/lepus/issues/71)

## Ergonomic APIs
AMQP protocol has a broad range of operations that can be used together to do a lot of interesting messaging,
however not all of the aforementioned combinations are allowed or even meaningful, which is annoying when using official client.
This client on the other hand, tries to have helpful and friendly APIs which guides the beginner, and helps the advanced user; 
without any sacrificing any meaningful power.

## Cross platform
To this date, this library is the only AMQP scala library that supports all scala platforms.

## Streaming
Built on top of fs2 and cats effect, this library never blocks, never uses reflection or any black magic.

## Out of the box utilities
Standard library and available integrations aim to provide friendly and easy to use tools which adhere to best practices
in messaging systems.

## Extensible
You can extend and/or integrate library in user land, examples include pluggable SASL mechanisms, 
custom codecs and even protocol frame inspector.
