package lepus.core

import fs2.Stream
import cats.effect.Resource

trait Connection[F[_]] {
  def maxChannels: F[Long]
  def frameMax: F[Long]
  //TODO do these need to be in F[_]
  def clientProperties: F[Map[String, Any]]

  def createChannel: Resource[F, Channel[F]]
  def createChannel(number: ChannelNumber): Resource[F, Channel[F]]

  def close(code: Int, message: String): F[Unit]
  def abort: F[Unit]
  def abort(code: Int, message: String): F[Unit]

  def blocked: Stream[F, String]
  def getId: F[ConnectionId]
}

type ExchangeName = String
type QueueName = String
type RoutingKey = String
type ChannelNumber = Int
type ConnectionId = String
type ExchangeType
type QDeclareOK
type BindArgs
type DeliveryTag = Long
type ConsumerTag = Long
type PublishSeqNr = Long
