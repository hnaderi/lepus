# Advanced

## Extending authentication (e.g. external SASL mechanism)

Connection constructor takes an @:api(lepus.client.AuthenticationConfig), which contains 
a list of @:api(lepus.client.SaslMechanism)s by order of preference,
default connection only contains a plain mechanism that uses username and password,
you can however implement any other mechanisms or integrate with other external mechanisms.

## Connection config

You can tune each connection using @:api(lepus.client.ConnectionConfig)

```scala mdoc:silent
import cats.effect.IO
import lepus.client.*
import lepus.protocol.domains.*

val config = ConnectionConfig.default
  .withName(ShortString("local-dev")) // set a connection name
  .withBufferSize(1000) // connection buffer size
  .withChannelConfig(  // global config for channels
    ChannelConfig.default.withDeliveryBufSize(1000) //channel delivery buffer size
  )
  
val connection = LepusClient[IO](config = config)
```

You can find all the options in @:api(lepus.client.ConnectionConfig) and @:api(lepus.client.ChannelConfig)

## Wire protocol
TBD
