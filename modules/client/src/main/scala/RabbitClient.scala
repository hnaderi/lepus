package lepus.client.java

import com.rabbitmq.*
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.GetResponse
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer

final class RabbitClient[F[_]] {
  val con: client.Connection = ???
  val ch: client.Channel = ???

  con.notifyListeners
  ch.addShutdownListener(???)
  val decOk: AMQP.Exchange.DeclareOk = ch.exchangeDeclare("", "")
  val msg: GetResponse = ch.basicGet("", false)
  val props: com.rabbitmq.client.AMQP.BasicProperties = msg.getProps
  val envelop: Envelope = msg.getEnvelope

  val consumer: Consumer = ch.getDefaultConsumer
  val consumer2: DefaultConsumer = ???

}
