package lepus.core

import fs2.Stream

trait ExchangeOperations[F[_]] {
  def exchangeDeclare(
      exchange: ExchangeName,
      exchangeType: ExchangeType
  ): F[Unit]
  //TODO Are these kind of methods necessary?
  def exchangeDeclareNoWait(
      exchange: ExchangeName,
      exchangeType: ExchangeType
  ): F[Unit]
  def exchangeDeclarePassive(
      exchange: ExchangeName,
      exchangeType: ExchangeType
  ): F[Unit]
  def exchangeDelete(
      exchange: ExchangeName,
      ifUnused: Boolean
  ): F[Unit]
  def exchangeDeleteNoWait(
      exchange: ExchangeName,
      ifUnused: Boolean
  ): F[Unit]
  def exchangeBind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: RoutingKey
  ): F[Unit]
  def exchangeBindNoWait(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: RoutingKey
  ): F[Unit]
  def exchangeUnbind(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: RoutingKey
  ): F[Unit]
  def exchangeUnbindNoWait(
      destination: ExchangeName,
      source: ExchangeName,
      routingKey: RoutingKey
  ): F[Unit]

}
