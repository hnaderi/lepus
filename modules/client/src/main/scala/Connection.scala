package lepus.client

import lepus.protocol.frame.*
import cats.effect.Concurrent
import cats.effect.Resource
import fs2.Stream
import fs2.Pipe
import cats.effect.std.Queue
import cats.effect.implicits.*

trait Connection[F[_]] {
  def openChannel: Resource[F, Channel[F]]
}

object Connection {
  def apply[F[_]: Concurrent](
      transport: Transport[F]
  ): Resource[F, Connection[F]] = for {
    sendQ <- Resource.eval(Queue.bounded[F, Frame](10))
    recvQ <- Resource.eval(Queue.bounded[F, Frame](10))
    _ <- Stream
      .repeatEval(sendQ.take)
      .through(transport)
      .evalMap(recvQ.offer)
      .compile
      .drain
      .background
  } yield ???
}
