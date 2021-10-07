package lepus.protocol.constants

val FrameMethod: Short = 1 //
val FrameHeader: Short = 2 //
val FrameBody: Short = 3 //
val FrameHeartbeat: Short = 8 //
val FrameMinSize: Short = 4096 //
val FrameEnd: Short = 206 //

enum ReplyCode(val code: Short) {
  case ReplySuccess extends ReplyCode(200) //
  case ContentTooLarge extends ReplyCode(311) // soft-error
  case NoConsumers extends ReplyCode(313) // soft-error
  case ConnectionForced extends ReplyCode(320) // hard-error
  case InvalidPath extends ReplyCode(402) // hard-error
  case AccessRefused extends ReplyCode(403) // soft-error
  case NotFound extends ReplyCode(404) // soft-error
  case ResourceLocked extends ReplyCode(405) // soft-error
  case PreconditionFailed extends ReplyCode(406) // soft-error
  case FrameError extends ReplyCode(501) // hard-error
  case SyntaxError extends ReplyCode(502) // hard-error
  case CommandInvalid extends ReplyCode(503) // hard-error
  case ChannelError extends ReplyCode(504) // hard-error
  case UnexpectedFrame extends ReplyCode(505) // hard-error
  case ResourceError extends ReplyCode(506) // hard-error
  case NotAllowed extends ReplyCode(530) // hard-error
  case NotImplemented extends ReplyCode(540) // hard-error
  case InternalError extends ReplyCode(541) // hard-error
}
