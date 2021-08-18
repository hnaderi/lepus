package lepus.core

import fs2.Stream

trait QueueOperations[F[_]] {
  def queueDeclare: F[QDeclareOK]
  def queueDeclare(name: QueueName): F[QDeclareOK]
  def queueDeclareNoWait(name: QueueName): F[QDeclareOK]
  def queueDeclarePassive(name: QueueName): F[QDeclareOK]
  def queueDelete(
      name: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean
  ): F[Unit]
  def queueDeleteNoWait(
      name: QueueName,
      ifUnused: Boolean,
      ifEmpty: Boolean
  ): F[Unit]
  def queueBind(
      destination: QueueName,
      source: ExchangeName,
      routingKey: RoutingKey
  ): F[Unit]
  def queueBind(
      destination: QueueName,
      source: ExchangeName,
      routingKey: RoutingKey,
      args: BindArgs
  ): F[Unit]
  def queueBindNoWait(
      destination: QueueName,
      source: ExchangeName,
      routingKey: RoutingKey,
      args: BindArgs
  ): F[Unit]
  def queueUnbind(
      destination: QueueName,
      source: ExchangeName,
      routingKey: RoutingKey
  ): F[Unit]
  def queueUnbind(
      destination: QueueName,
      source: ExchangeName,
      routingKey: RoutingKey,
      args: BindArgs
  ): F[Unit]
  def queuePurge(name: QueueName): F[Unit]
  def messageCount(name: QueueName): F[Long]
  def consumerCount(name: QueueName): F[Long]
}
