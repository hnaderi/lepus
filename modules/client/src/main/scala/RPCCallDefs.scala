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
package internal

import cats.MonadError
import cats.implicits.*
import lepus.protocol.*
import lepus.protocol.domains.*

import scala.annotation.implicitNotFound

@implicitNotFound(
  "${M} is not a client side method, or you can't use ${F} as an effect for rpc calls"
)
private[client] sealed trait RPCCallDef[F[_], M <: Method, O] {
  def call(rpc: ChannelTransmitter[F])(i: M): F[O]
}

private[client] object RPCCallDef {
  given ConnectionClass_StartOk[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.StartOk, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.StartOk
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ConnectionClass_SecureOk[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.SecureOk, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.SecureOk
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ConnectionClass_TuneOk[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.TuneOk, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: ConnectionClass.TuneOk): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given ConnectionClass_Open[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.Open, ConnectionClass.OpenOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.Open
    ): F[ConnectionClass.OpenOk.type] = rpc.sendWait(msg).flatMap {
      case m: ConnectionClass.OpenOk.type => m.pure
      case other                          =>
        F.raiseError(UnexpectedResponse(other, ClassId(10), MethodId(40)))
    }
  }

  given ConnectionClass_Close[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.Close, ConnectionClass.CloseOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.Close
    ): F[ConnectionClass.CloseOk.type] = rpc.sendWait(msg).flatMap {
      case m: ConnectionClass.CloseOk.type => m.pure
      case other                           =>
        F.raiseError(UnexpectedResponse(other, ClassId(10), MethodId(50)))
    }
  }

  given ConnectionClass_CloseOk_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.CloseOk.type, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.CloseOk.type
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ConnectionClass_Blocked[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.Blocked, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.Blocked
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ConnectionClass_Unblocked_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.Unblocked.type, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.Unblocked.type
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ConnectionClass_UpdateSecretOk_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConnectionClass.UpdateSecretOk.type, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ConnectionClass.UpdateSecretOk.type
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ChannelClass_Open_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ChannelClass.Open.type, ChannelClass.OpenOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ChannelClass.Open.type
    ): F[ChannelClass.OpenOk.type] = rpc.sendWait(msg).flatMap {
      case m: ChannelClass.OpenOk.type => m.pure
      case other                       =>
        F.raiseError(UnexpectedResponse(other, ClassId(20), MethodId(10)))
    }
  }

  given ChannelClass_Flow[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ChannelClass.Flow, ChannelClass.FlowOk] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ChannelClass.Flow
    ): F[ChannelClass.FlowOk] = rpc.sendWait(msg).flatMap {
      case m: ChannelClass.FlowOk => m.pure
      case other                  =>
        F.raiseError(UnexpectedResponse(other, ClassId(20), MethodId(20)))
    }
  }

  given ChannelClass_FlowOk[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ChannelClass.FlowOk, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: ChannelClass.FlowOk): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given ChannelClass_Close[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ChannelClass.Close, ChannelClass.CloseOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ChannelClass.Close
    ): F[ChannelClass.CloseOk.type] = rpc.sendWait(msg).flatMap {
      case m: ChannelClass.CloseOk.type => m.pure
      case other                        =>
        F.raiseError(UnexpectedResponse(other, ClassId(20), MethodId(40)))
    }
  }

  given ChannelClass_CloseOk_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ChannelClass.CloseOk.type, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ChannelClass.CloseOk.type
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given ExchangeClass_Declare[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ExchangeClass.Declare, Option[
    ExchangeClass.DeclareOk.type
  ]] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: ExchangeClass.Declare
    ): F[Option[ExchangeClass.DeclareOk.type]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: ExchangeClass.DeclareOk.type => m.pure
          case other                           =>
            F.raiseError(UnexpectedResponse(other, ClassId(40), MethodId(10)))
        }
        .map(_.some)
  }

  given ExchangeClass_Delete[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ExchangeClass.Delete, Option[ExchangeClass.DeleteOk.type]] =
    new {
      def call(rpc: ChannelTransmitter[F])(
          msg: ExchangeClass.Delete
      ): F[Option[ExchangeClass.DeleteOk.type]] = if msg.noWait then
        rpc.sendNoWait(msg).as(None)
      else
        rpc
          .sendWait(msg)
          .flatMap {
            case m: ExchangeClass.DeleteOk.type => m.pure
            case other                          =>
              F.raiseError(UnexpectedResponse(other, ClassId(40), MethodId(20)))
          }
          .map(_.some)
    }

  given ExchangeClass_Bind[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ExchangeClass.Bind, Option[ExchangeClass.BindOk.type]] =
    new {
      def call(rpc: ChannelTransmitter[F])(
          msg: ExchangeClass.Bind
      ): F[Option[ExchangeClass.BindOk.type]] = if msg.noWait then
        rpc.sendNoWait(msg).as(None)
      else
        rpc
          .sendWait(msg)
          .flatMap {
            case m: ExchangeClass.BindOk.type => m.pure
            case other                        =>
              F.raiseError(UnexpectedResponse(other, ClassId(40), MethodId(30)))
          }
          .map(_.some)
    }

  given ExchangeClass_Unbind[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ExchangeClass.Unbind, Option[ExchangeClass.UnbindOk.type]] =
    new {
      def call(rpc: ChannelTransmitter[F])(
          msg: ExchangeClass.Unbind
      ): F[Option[ExchangeClass.UnbindOk.type]] = if msg.noWait then
        rpc.sendNoWait(msg).as(None)
      else
        rpc
          .sendWait(msg)
          .flatMap {
            case m: ExchangeClass.UnbindOk.type => m.pure
            case other                          =>
              F.raiseError(UnexpectedResponse(other, ClassId(40), MethodId(40)))
          }
          .map(_.some)
    }

  given QueueClass_Declare[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, QueueClass.Declare, Option[QueueClass.DeclareOk]] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: QueueClass.Declare
    ): F[Option[QueueClass.DeclareOk]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.DeclareOk => m.pure
          case other                   =>
            F.raiseError(UnexpectedResponse(other, ClassId(50), MethodId(10)))
        }
        .map(_.some)
  }

  given QueueClass_Bind[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, QueueClass.Bind, Option[QueueClass.BindOk.type]] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: QueueClass.Bind
    ): F[Option[QueueClass.BindOk.type]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.BindOk.type => m.pure
          case other                     =>
            F.raiseError(UnexpectedResponse(other, ClassId(50), MethodId(20)))
        }
        .map(_.some)
  }

  given QueueClass_Unbind[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, QueueClass.Unbind, QueueClass.UnbindOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: QueueClass.Unbind
    ): F[QueueClass.UnbindOk.type] = rpc.sendWait(msg).flatMap {
      case m: QueueClass.UnbindOk.type => m.pure
      case other                       =>
        F.raiseError(UnexpectedResponse(other, ClassId(50), MethodId(50)))
    }
  }

  given QueueClass_Purge[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, QueueClass.Purge, Option[QueueClass.PurgeOk]] = new {
    def call(
        rpc: ChannelTransmitter[F]
    )(msg: QueueClass.Purge): F[Option[QueueClass.PurgeOk]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.PurgeOk => m.pure
          case other                 =>
            F.raiseError(UnexpectedResponse(other, ClassId(50), MethodId(30)))
        }
        .map(_.some)
  }

  given QueueClass_Delete[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, QueueClass.Delete, Option[QueueClass.DeleteOk]] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: QueueClass.Delete
    ): F[Option[QueueClass.DeleteOk]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: QueueClass.DeleteOk => m.pure
          case other                  =>
            F.raiseError(UnexpectedResponse(other, ClassId(50), MethodId(40)))
        }
        .map(_.some)
  }

  given BasicClass_Qos[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Qos, BasicClass.QosOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: BasicClass.Qos
    ): F[BasicClass.QosOk.type] = rpc.sendWait(msg).flatMap {
      case m: BasicClass.QosOk.type => m.pure
      case other                    =>
        F.raiseError(UnexpectedResponse(other, ClassId(60), MethodId(10)))
    }
  }

  given BasicClass_Consume[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Consume, Option[BasicClass.ConsumeOk]] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: BasicClass.Consume
    ): F[Option[BasicClass.ConsumeOk]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: BasicClass.ConsumeOk => m.pure
          case other                   =>
            F.raiseError(UnexpectedResponse(other, ClassId(60), MethodId(20)))
        }
        .map(_.some)
  }

  given BasicClass_Cancel[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Cancel, Option[BasicClass.CancelOk]] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: BasicClass.Cancel
    ): F[Option[BasicClass.CancelOk]] = if msg.noWait then
      rpc.sendNoWait(msg).as(None)
    else
      rpc
        .sendWait(msg)
        .flatMap {
          case m: BasicClass.CancelOk => m.pure
          case other                  =>
            F.raiseError(UnexpectedResponse(other, ClassId(60), MethodId(30)))
        }
        .map(_.some)
  }

  given BasicClass_CancelOk[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.CancelOk, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: BasicClass.CancelOk): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given BasicClass_Publish[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Publish, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: BasicClass.Publish): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given BasicClass_Get[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[
    F,
    BasicClass.Get,
    BasicClass.GetOk | BasicClass.GetEmpty.type
  ] = new {
    def call(
        rpc: ChannelTransmitter[F]
    )(msg: BasicClass.Get): F[BasicClass.GetOk | BasicClass.GetEmpty.type] =
      rpc.sendWait(msg).flatMap {
        case m: BasicClass.GetOk         => m.pure
        case m: BasicClass.GetEmpty.type => m.pure
        case other                       =>
          F.raiseError(UnexpectedResponse(other, ClassId(60), MethodId(70)))
      }
  }

  given BasicClass_Ack[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Ack, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: BasicClass.Ack): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given BasicClass_Reject[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Reject, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: BasicClass.Reject): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given BasicClass_RecoverAsync[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.RecoverAsync, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: BasicClass.RecoverAsync
    ): F[Unit] = rpc.sendNoWait(msg)
  }

  given BasicClass_Recover[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Recover, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: BasicClass.Recover): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given BasicClass_Nack[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, BasicClass.Nack, Unit] = new {
    def call(rpc: ChannelTransmitter[F])(msg: BasicClass.Nack): F[Unit] =
      rpc.sendNoWait(msg)
  }

  given TxClass_Select_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, TxClass.Select.type, TxClass.SelectOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: TxClass.Select.type
    ): F[TxClass.SelectOk.type] = rpc.sendWait(msg).flatMap {
      case m: TxClass.SelectOk.type => m.pure
      case other                    =>
        F.raiseError(UnexpectedResponse(other, ClassId(90), MethodId(10)))
    }
  }

  given TxClass_Commit_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, TxClass.Commit.type, TxClass.CommitOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: TxClass.Commit.type
    ): F[TxClass.CommitOk.type] = rpc.sendWait(msg).flatMap {
      case m: TxClass.CommitOk.type => m.pure
      case other                    =>
        F.raiseError(UnexpectedResponse(other, ClassId(90), MethodId(20)))
    }
  }

  given TxClass_Rollback_type[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, TxClass.Rollback.type, TxClass.RollbackOk.type] = new {
    def call(rpc: ChannelTransmitter[F])(
        msg: TxClass.Rollback.type
    ): F[TxClass.RollbackOk.type] = rpc.sendWait(msg).flatMap {
      case m: TxClass.RollbackOk.type => m.pure
      case other                      =>
        F.raiseError(UnexpectedResponse(other, ClassId(90), MethodId(30)))
    }
  }

  given ConfirmClass_Select[F[_]](using
      F: MonadError[F, Throwable]
  ): RPCCallDef[F, ConfirmClass.Select, Option[ConfirmClass.SelectOk.type]] =
    new {
      def call(rpc: ChannelTransmitter[F])(
          msg: ConfirmClass.Select
      ): F[Option[ConfirmClass.SelectOk.type]] = if msg.noWait then
        rpc.sendNoWait(msg).as(None)
      else
        rpc
          .sendWait(msg)
          .flatMap {
            case m: ConfirmClass.SelectOk.type => m.pure
            case other                         =>
              F.raiseError(UnexpectedResponse(other, ClassId(85), MethodId(10)))
          }
          .map(_.some)
    }

}
