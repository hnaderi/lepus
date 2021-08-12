package lepus.core

final case class Message(
    consumer: ConsumerTag,
    envelope: Envelope,
    properties: BasicProperties,
    body: Array[Byte]
)
