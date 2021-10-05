package lepus.core

import fs2.Stream
import cats.effect.Resource

trait BasicOperations[F[_]] {
  def qos(prefetchSize: Int, prefetchCount: Int, global: Boolean): F[Unit]
  def qos(prefetchCount: Int, global: Boolean): F[Unit]
  def qos(prefetchCount: Int): F[Unit]

  def publish(
      exchange: ExchangeName,
      routingKey: RoutingKey,
      properties: BasicProperties,
      body: Array[Byte]
  ): F[Unit]
  def publish(
      exchange: ExchangeName,
      routingKey: RoutingKey,
      mandatory: Boolean,
      properties: BasicProperties,
      body: Array[Byte]
  ): F[Unit]
  def publish(
      exchange: ExchangeName,
      routingKey: RoutingKey,
      mandatory: Boolean,
      immediate: Boolean,
      properties: BasicProperties,
      body: Array[Byte]
  ): F[Unit]

  def ack(tag: DeliveryTag, multiple: Boolean): F[Unit]
  def nack(tag: DeliveryTag, multiple: Boolean): F[Unit]
  def reject(tag: DeliveryTag, multiple: Boolean): F[Unit]

  def consumeAutoAck(name: QueueName): Resource[F, Consumer[F]]
  def consumeAck(name: QueueName): Resource[F, Consumer[F]]
  //A lot more basic consumes!

  def recover(requeue: Boolean): F[Unit]
}
