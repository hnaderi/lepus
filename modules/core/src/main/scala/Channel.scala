package lepus.core

import fs2.Stream

trait Channel[F[_]] {
  def number: F[ChannelNumber]
  def connection: F[Connection[F]]
  def close(code: Int, message: String): F[Unit]
  def abort: F[Unit]
  def abort(code: Int, message: String): F[Unit]

  def returns: Stream[F, Unit]
  def confirms: Stream[F, Unit]

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

  def basicAck(tag: DeliveryTag, multiple: Boolean): F[Unit]
  def basicNack(tag: DeliveryTag, multiple: Boolean): F[Unit]
  def basicReject(tag: DeliveryTag, multiple: Boolean): F[Unit]

  def basicConsumeAutoAck(name: QueueName): Stream[F, Message]
  def basicConsumeAck(name: QueueName): Stream[F, Message]
  //A lot more basic consumes!

  def basicRecover(requeue: Boolean): F[Unit]

  def txBegin: F[Unit]
  def txCommit: F[Unit]
  def txRollback: F[Unit]

  def confirmSelect: F[Unit]
  def nextPublishSeqNr: F[PublishSeqNr]
  def waitForConfirms: F[Boolean]

  //TODO: What is this? low level methods?
  def rpc(method: Any): F[Boolean]

  def messageCount(name: QueueName): F[Long]
  def consumerCount(name: QueueName): F[Long]

}
