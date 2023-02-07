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
import cats.effect.kernel.Ref
import cats.effect.std.QueueSink
import cats.syntax.all.*
import fs2.Pipe
import fs2.Pull
import fs2.Stream
import lepus.protocol.ConnectionClass
import lepus.protocol.ConnectionClass.Secure
import lepus.protocol.ConnectionClass.Start
import lepus.protocol.ConnectionClass.Tune
import lepus.protocol.Frame
import lepus.protocol.Method
import lepus.protocol.domains.*

trait StartupNegotiation[F[_]] {
  def pipe(sendQ: Frame => F[Unit]): Pipe[F, Frame, Frame]
  def config: F[Option[NegotiatedConfig]]
}
object StartupNegotiation {
  def apply[F[_]: Concurrent](
      auth: AuthenticationConfig[F],
      vhost: Path = Path("/")
  ): F[StartupNegotiation[F]] = for {
    conf <- Deferred[F, Option[NegotiatedConfig]]
  } yield new StartupNegotiation[F] {

    override def pipe(sendQ: Frame => F[Unit]): Pipe[F, Frame, Frame] = in => {
      def go(
          step: Negotiation[F],
          frames: Stream[F, Frame]
      ): Pull[F, Frame, Unit] =
        frames.pull.uncons1.flatMap {
          case Some((frame, nextFrames)) =>
            Pull
              .eval(step(frame).onError(_ => conf.complete(None).void))
              .flatMap {
                case NegotiationResult.Continue(response, nextStep) =>
                  Pull
                    .eval(sendQ(response)) >> go(nextStep, nextFrames)
                case NegotiationResult.Completed(response, config) =>
                  Pull.eval(
                    sendQ(response) >> conf.complete(Some(config))
                  ) >> nextFrames.pull.echo
              }
          case None => Pull.eval(conf.complete(None).void)
        }

      go(start, in).stream
    }

    override def config: F[Option[NegotiatedConfig]] = conf.get

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
        auth.get(proposedMechanisms: _*) match {
          case None => NoSupportedSASLMechanism.raiseError
          case Some((mechanism, sasl)) =>
            sasl.first.map(response =>
              NegotiationResult.continue(
                ConnectionClass.StartOk(
                  clientProps,
                  mechanism,
                  response,
                  ShortString("en-US")
                )
              )(handleChallenge(sasl))
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

  // TODO real properties
  private val clientProps = FieldTable(
    ShortString("product") -> 1,
    ShortString("version") -> 1,
    ShortString("platform") -> 1
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
case object NoSupportedSASLMechanism
    extends Exception(
      "Server does not support any of your requested SASL mechanisms!"
    )
