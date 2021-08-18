package lepus.core

final case class Message(
    envelope: Envelope,
    properties: BasicProperties,
    body: Array[Byte]
)
