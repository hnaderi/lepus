package lepus.core

import java.time.Instant

final case class Envelope(
    tag: DeliveryTag,
    redeliver: Boolean,
    exchange: ExchangeName,
    routingKey: RoutingKey
)
