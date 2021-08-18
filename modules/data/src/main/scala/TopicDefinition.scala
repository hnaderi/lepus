package lepus.data

import lepus.core.*

final case class TopicDefinition[T](
    exchange: ExchangeName,
    codec: MessageCodec[T]
)
