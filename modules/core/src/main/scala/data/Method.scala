package lepus.core

final case class Method(
    classId: ClassId,
    methodId: MethodId,
    arguments: List[AMQPField]
)
