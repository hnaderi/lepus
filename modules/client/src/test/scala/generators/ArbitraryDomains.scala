package lepus.codecs

import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import org.scalacheck.Arbitrary

import DomainGenerators.*

object ArbitraryDomains {
  given Arbitrary[ExchangeName] = Arbitrary(exchangeName)
  given Arbitrary[QueueName] = Arbitrary(queueName)
  given Arbitrary[MethodId] = Arbitrary(methodIds)
  given Arbitrary[ClassId] = Arbitrary(classIds)
  given Arbitrary[ShortString] = Arbitrary(shortString)
  given Arbitrary[LongString] = Arbitrary(longString)
  given Arbitrary[Timestamp] = Arbitrary(timestamp)
  given Arbitrary[ConsumerTag] = Arbitrary(consumerTag)
  given Arbitrary[MessageCount] = Arbitrary(messageCount)
  given Arbitrary[Path] = Arbitrary(path)
  given Arbitrary[DeliveryTag] = Arbitrary(deliveryTag)
  given Arbitrary[DeliveryMode] = Arbitrary(deliveryMode)
  given Arbitrary[Priority] = Arbitrary(priority)
  given Arbitrary[Decimal] = Arbitrary(decimal)
  given Arbitrary[FieldData] = Arbitrary(fieldData)
  given Arbitrary[FieldTable] = Arbitrary(fieldTable)
  given Arbitrary[ReplyCode] = Arbitrary(replyCode)
}
