package lepus.core

import fs2.Stream
import cats.effect.Resource

trait BasicOperations[F[_]] {
  def basicQos(prefetchSize: Int, prefetchCount: Int, global: Boolean): F[Unit]
  def basicQos(prefetchCount: Int, global: Boolean): F[Unit]
  def basicQos(prefetchCount: Int): F[Unit]

  def basicPublish(
      exchange: ExchangeName,
      routingKey: RoutingKey,
      properties: BasicProperties,
      body: Array[Byte]
  ): F[Unit]
  def basicPublish(
      exchange: ExchangeName,
      routingKey: RoutingKey,
      mandatory: Boolean,
      properties: BasicProperties,
      body: Array[Byte]
  ): F[Unit]
  def basicPublish(
      exchange: ExchangeName,
      routingKey: RoutingKey,
      mandatory: Boolean,
      immediate: Boolean,
      properties: BasicProperties,
      body: Array[Byte]
  ): F[Unit]

  def basicAck(tag: DeliveryTag, multiple: Boolean): F[Unit]
  def basicNack(tag: DeliveryTag, multiple: Boolean): F[Unit]
  def basicReject(tag: DeliveryTag, multiple: Boolean): F[Unit]

  def basicConsumeAutoAck(name: QueueName): Resource[F, Consumer[F]]
  def basicConsumeAck(name: QueueName): Resource[F, Consumer[F]]
  //A lot more basic consumes!

  def basicRecover(requeue: Boolean): F[Unit]
}
