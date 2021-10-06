package lepus.data

final case class TopicDefinition[T](
    exchange: String,
    codec: MessageCodec[T]
)
