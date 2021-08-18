package lepus.std

trait Bus[F[_], T] {
  def publish(t: T): F[Unit]
}
