package lepus.protocol.method

import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Exchange {
  case Declare(
      reserved1: Unit,
      exchange: ExchangeName,
      etype: ShortString,
      passive: Boolean,
      durable: Boolean,
      reserved2: Unit,
      reserved3: Unit,
      noWait: NoWait,
      arguments: FieldTable
  )
  case DeclareOk()
  case Delete(
      reserved1: Unit,
      exchange: ExchangeName,
      ifUnused: Boolean,
      noWait: NoWait
  )
  case DeleteOk()
}
