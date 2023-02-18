/*
 * Copyright 2021 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lepus.std

import cats.effect.Concurrent
import cats.syntax.all.*
import fs2.Stream
import lepus.client.*
import lepus.client.apis.NormalMessagingChannel
import lepus.protocol.domains.*

trait RPCServer[F[_], I, O] {
  def requests: Stream[F, RequestMethod[I]]
  def respond(req: RequestMethod[I], o: O): F[Unit]
  def ignore(i: RequestMethod[I]): F[Unit]
  def reject(i: RequestMethod[I]): F[Unit]
}

trait RPCClient[F[_], I, O] {
  def send(id: ShortString, i: I): F[Unit]
  def responses: Stream[F, ResponseMethod[O]]
  def processed(i: ResponseMethod[O]): F[Unit]
}

final case class RequestMethod[I](
    id: ShortString,
    sender: QueueName,
    payload: I,
    tag: DeliveryTag
)
final case class ResponseMethod[I](
    requestId: ShortString,
    payload: I,
    tag: DeliveryTag
)

object RPCChannel {
  def server[F[_]: Concurrent, I, O](
      endpoint: EndpointDefinition[I, O]
  )(ch: Channel[F, NormalMessagingChannel[F]]): F[RPCServer[F, I, O]] = for {
    _ <- ch.queue.declare(endpoint.name, durable = true)
  } yield new {

    override def respond(req: RequestMethod[I], o: O): F[Unit] =
      endpoint.serverCodec.encode(o) match {
        case Left(error) => error.raiseError
        case Right(msg) =>
          ch.messaging.publishRaw(
            ExchangeName.default,
            routingKey = req.sender,
            msg
          ) >> ch.messaging.ack(req.tag, false)
      }

    override def requests: Stream[F, RequestMethod[I]] = ch.messaging
      .consumeRaw(endpoint.name, noAck = false)
      .flatMap(env =>
        endpoint.clientCodec.decode(env.message) match
          case Left(error @ _) =>
            Stream.exec(reject(env.deliveryTag))
          case Right(value) =>
            val senderQ =
              value.properties.replyTo
                .flatMap(QueueName.from(_).toOption)

            val msgId = value.properties.messageId

            msgId
              .zip(senderQ)
              .fold(Stream.exec(reject(env.deliveryTag))) { (id, sender) =>
                Stream.emit(
                  RequestMethod(
                    id,
                    sender,
                    value.payload,
                    env.deliveryTag
                  )
                )
              }
      )

    override def ignore(i: RequestMethod[I]): F[Unit] =
      ch.messaging.ack(i.tag)

    override def reject(i: RequestMethod[I]): F[Unit] = reject(i.tag)

    private def reject(dtag: DeliveryTag) =
      ch.messaging.reject(dtag, false)

  }

  def client[F[_], I, O](
      endpoint: EndpointDefinition[I, O],
      persistent: Option[QueueName] = None
  )(
      ch: Channel[F, NormalMessagingChannel[F]]
  )(using F: Concurrent[F]): F[RPCClient[F, I, O]] = for {
    q <- persistent match {
      case None =>
        ch.queue
          .declare(QueueName.autoGen, exclusive = true)
          .flatMap(
            F.fromOption(_, new UnknownError("Must respond with Queue name"))
          )
          .map(_.queue)
      case Some(value) => ch.queue.declare(value, durable = true).as(value)
    }
  } yield new {

    override def send(id: ShortString, i: I): F[Unit] = endpoint.clientCodec
      .encode(i)
      .fold(
        _.raiseError,
        msg =>
          ch.messaging.publishRaw(
            ExchangeName.default,
            routingKey = endpoint.name,
            msg.withMessageId(id).withReplyTo(q)
          )
      )

    override def responses: Stream[F, ResponseMethod[O]] = ch.messaging
      .consumeRaw(q, noAck = false)
      .flatMap(env =>
        endpoint.serverCodec.decode(env.message) match
          case Left(error) => Stream.exec(reject(env.deliveryTag))
          case Right(value) =>
            value.properties.correlationId
              .fold(Stream.exec(reject(env.deliveryTag)))(responseTo =>
                Stream.emit(
                  ResponseMethod(responseTo, value.payload, env.deliveryTag)
                )
              )
      )

    override def processed(i: ResponseMethod[O]): F[Unit] =
      ch.messaging.ack(i.tag)

    private def reject(dtag: DeliveryTag) =
      ch.messaging.reject(dtag, requeue = false)

  }

}
