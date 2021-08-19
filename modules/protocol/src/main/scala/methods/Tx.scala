package lepus.protocol.method

import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Tx {
  case Select()
  case SelectOk()
  case Commit()
  case CommitOk()
  case Rollback()
  case RollbackOk()
}
