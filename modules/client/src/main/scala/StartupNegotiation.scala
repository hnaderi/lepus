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

package lepus.client

import cats.effect.Concurrent
import cats.effect.kernel.Deferred
import cats.syntax.all.*
import fs2.Pipe
import fs2.Pull
import fs2.Stream
import lepus.protocol.ConnectionClass
import lepus.protocol.ConnectionClass.Close
import lepus.protocol.ConnectionClass.Secure
import lepus.protocol.ConnectionClass.Start
import lepus.protocol.ConnectionClass.Tune
import lepus.protocol.Frame
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*

trait StartupNegotiation[F[_]] {
  def pipe(sendQ: Frame => F[Unit]): Pipe[F, Frame, Frame]
  def config: F[NegotiatedConfig]
  def capabilities: F[Capabilities]
}
object StartupNegotiation {
  def apply[F[_]: Concurrent](
      auth: AuthenticationConfig[F],
      vhost: Path = Path("/"),
      connectionName: Option[ShortString] = None
  ): F[StartupNegotiation[F]] = for {
    conf <- Deferred[F, Either[Throwable, NegotiatedConfig]]
    caps <- Deferred[F, Either[Throwable, Capabilities]]
  } yield new StartupNegotiation[F] {

    override def capabilities: F[Capabilities] = caps.get.flatMap(_.liftTo)

    private def terminate(ex: Throwable) =
      conf.complete(Left(ex)) >> caps.complete(Left(ex)).void

    override def pipe(sendQ: Frame => F[Unit]): Pipe[F, Frame, Frame] = in => {
      def go(
          step: Negotiation[F],
          frames: Stream[F, Frame]
      ): Pull[F, Frame, Unit] =
        frames.pull.uncons1
          .onError(ex => Pull.eval(terminate(ex)))
          .flatMap {
            case Some((frame, nextFrames)) =>
              Pull
                .eval(step(frame).onError(terminate(_)))
                .flatMap {
                  case NegotiationResult.Continue(response, nextStep) =>
                    Pull
                      .eval(sendQ(response)) >> go(nextStep, nextFrames)
                  case NegotiationResult.Completed(response, config) =>
                    Pull.eval(
                      sendQ(response) >> conf.complete(Right(config))
                    ) >> nextFrames.pull.echo
                }
            case None => Pull.eval(terminate(NegotiationFailed))
          }

      go(start, in).stream
    }

    override def config: F[NegotiatedConfig] = conf.get.flatMap(_.liftTo)

    private def method(
        f: PartialFunction[ConnectionClass, F[NegotiationResult[F]]]
    ): Negotiation[F] = {
      case Frame.Method(0, method: ConnectionClass) =>
        f.lift(method).getOrElse(NegotiationError.raiseError)
      case _ => NegotiationError.raiseError
    }

    private def start: Negotiation[F] = method {
      case Start(0, 9, serverProperties, mechanisms, locales) =>
        val proposedMechanisms = mechanisms.split(" ")
        val serverCaps = serverProperties
          .get(ShortString("capabilities"))
          .collect { case t: FieldTable => t }
          .fold(Capabilities.none)(Capabilities.from(_))

        auth.get(proposedMechanisms: _*) match {
          case None => NoSupportedSASLMechanism.raiseError
          case Some(mechanism) =>
            caps.complete(Right(serverCaps)) >>
              mechanism.first.map(response =>
                NegotiationResult.continue(
                  ConnectionClass.StartOk(
                    clientProps(connectionName),
                    mechanism.name,
                    response,
                    ShortString("en-US")
                  )
                )(handleChallenge(mechanism))
              )
        }

    }
    private def handleChallenge(sasl: SaslMechanism[F]): Negotiation[F] =
      method {
        case Secure(challenge) =>
          sasl
            .next(challenge)
            .map(response =>
              NegotiationResult
                .continue(ConnectionClass.SecureOk(response))(
                  handleChallenge(sasl)
                )
            )
        case msg: Tune => afterChallenge(msg)
        case Close(ReplyCode.AccessRefused, details, _, _) =>
          AuthenticationFailure(details).raiseError
      }
    private def afterChallenge
        : ConnectionClass.Tune => F[NegotiationResult[F]] = {
      case Tune(channelMax, frameMax, heartbeat) =>
        NegotiationResult
          .completed(
            NegotiatedConfig(
              channelMax = channelMax,
              frameMax = frameMax,
              heartbeat = heartbeat
            )
          )
          .pure[F]
    }

  }

  private[client] def clientProps(connectionName: Option[ShortString]) =
    FieldTable(
      ShortString("product") -> ShortString("Lepus"),
      ShortString("version") -> ShortString(BuildInfo.version),
      ShortString("platform") -> ShortString("scala"),
      ShortString("scala-version") -> ShortString(BuildInfo.scalaVersion),
      ShortString("capabilities") -> FieldTable(
        ShortString("publisher_confirms") -> true,
        ShortString("authentication_failure_close") -> true,
        ShortString("consumer_cancel_notify") -> true,
        ShortString("basic.nack") -> true,
        ShortString("connection.blocked") -> true
      ).updated(ShortString("connection_name"), connectionName)
    )
}

final case class NegotiatedConfig(
    channelMax: Short,
    frameMax: Int,
    heartbeat: Short
)

private enum NegotiationResult[F[_]] {
  case Continue(response: Frame, next: Negotiation[F])
  case Completed(response: Frame, config: NegotiatedConfig)
}

private object NegotiationResult {
  def continue[F[_]](response: ConnectionClass)(next: Negotiation[F]) =
    Continue(Frame.Method(ChannelNumber(0), response), next)
  def completed[F[_]](config: NegotiatedConfig) =
    Completed[F](
      Frame.Method(
        ChannelNumber(0),
        ConnectionClass
          .TuneOk(config.channelMax, config.frameMax, config.heartbeat)
      ),
      config
    )
}

type Negotiation[F[_]] = Frame => F[NegotiationResult[F]]

case object NegotiationError
    extends Exception("Error while negotiating with server!")
case class AuthenticationFailure(details: String)
    extends Exception(
      s"Server refused connection due to authentication failure!\nDetails: $details"
    )
case object NoSupportedSASLMechanism
    extends Exception(
      "Server does not support any of your requested SASL mechanisms!"
    )
case object NegotiationFailed
    extends Exception("Negotiation with server failed!")
