//> using scala 3.3
//> using dep "io.circe::circe-generic:0.14.8"
//> using dep "dev.hnaderi::named-codec-circe:0.1.0"
//> using dep "dev.hnaderi::lepus-std:0.5.5"
//> using dep "dev.hnaderi::lepus-circe:0.5.5"

package example

import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import lepus.client.*
import lepus.protocol.domains.*
import lepus.std.*

object RPC extends IOApp {

  private val protocol =
    RPCDefinition(
      QueueName("rpc"),
      ChannelCodec.plain(MessageCodec.json[Request]),
      ChannelCodec.plain(MessageCodec.json[Response])
    )

  private val connection = Stream.resource(LepusClient[IO]())
  private val channel = connection.flatMap(con => Stream.resource(con.channel))

  val server =
    channel
      .evalMap(RPCChannel.server(protocol))
      .flatMap(rpc =>
        rpc.requests
          .evalTap(IO.println)
          .evalMap(req => rpc.respond(req, Response(s"result[${req.payload}]")))
      )

  val client = channel
    .evalMap(RPCChannel.client(protocol))
    .flatMap(rpc =>
      rpc.responses
        .evalMap { job =>
          IO.println(s"name: $job") >>
            rpc.processed(job)
        }
        .concurrently(
          fs2.io
            .stdinUtf8[IO](100)
            .zipWithIndex
            .evalMap((s, i) => rpc.send(ShortString.from(i), Request(s)))
        )
    )

  override def run(args: List[String]): IO[ExitCode] =
    (args.map(_.toLowerCase) match {
      case "server" :: _ => server
      case "client" :: _ => client
      case _             => Stream.exec(IO.println(s"""Usage: rpc command
Commands:
  - server
  - client
"""))
    }).compile.drain.as(ExitCode.Success)
}

final case class Request(value: String) derives io.circe.Codec.AsObject
final case class Response(value: String) derives io.circe.Codec.AsObject
