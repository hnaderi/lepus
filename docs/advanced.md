# Advanced

## Extending authentication (e.g. external SASL mechanism)

Connection constructor takes an @:api(lepus.client.AuthenticationConfig), which contains 
a list of @:api(lepus.client.SaslMechanism)s by order of preference,
default connection only contains a plain mechanism that uses username and password,
you can however implement any other mechanisms or integrate with other external mechanisms.

## Wire protocol
TBD
