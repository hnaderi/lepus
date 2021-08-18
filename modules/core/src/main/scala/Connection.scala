package lepus.core

import fs2.Stream
import cats.effect.Resource
import lepus.core.TerminateOperations

trait Connection[F[_]] extends TerminateOperations[F] {
  def maxChannels: F[Long]
  def frameMax: F[Long]
  def clientProperties: F[Map[String, Any]]

  def createChannel: Resource[F, Channel[F]]
  def createChannel(number: ChannelNumber): Resource[F, Channel[F]]

  def blocked: Stream[F, String]
  def getId: F[ConnectionId]
}
