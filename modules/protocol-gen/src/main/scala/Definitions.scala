package lepus.protocol.gen

enum PrimitiveType {
  case bit, octet, short, long, longlong, shortstr, longstr, timestamp, table
}

final case class Domain(
    name: String,
    dataType: PrimitiveType,
    label: String,
    doc: Option[String],
    assertions: List[String]
)

final case class Class(
    name: String,
    id: Short,
    label: String,
    doc: String,
    methods: List[Method]
)

enum MethodType {
  case Sync, ASync
}

enum MethodReceiver {
  case Server, Client, Both
}

final case class Method(
    name: String,
    id: Short,
    label: String,
    sync: MethodType,
    receiver: MethodReceiver,
    doc: String,
    fields: List[Field]
)

final case class Field(
    name: String,
    label: String,
    dataType: String,
    doc: String,
    reserved: Boolean
)
