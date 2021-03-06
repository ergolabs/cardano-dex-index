package fi.spectrumlabs.db.writer.persistence

import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import cats.{Applicative, FlatMap}
import doobie.ConnectionIO
import doobie.util.Write
import doobie.util.log.LogHandler
import fi.spectrumlabs.db.writer.schema.Schema
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.higherKind.RepresentableK

/** Takes batch of T elements and persists them into indexes storage.
  */
trait Persist[T, F[_]] {
  def persist(inputs: NonEmptyList[T]): F[Int]
}

object Persist {

  implicit def repK[T]: RepresentableK[Persist[T, *[_]]] = {
    type Repr[F[_]] = Persist[T, F]
    tofu.higherKind.derived.genRepresentableK[Repr]
  }

  def create[T: Write, D[_]: FlatMap: LiftConnectionIO, F[_]: Applicative](schema: Schema[T])(implicit
    elh: EmbeddableLogHandler[D],
    txr: Txr[F, D]
  ): Persist[T, F] =
    elh.embed(implicit __ => new Impl[T](schema).mapK(LiftConnectionIO[D].liftF)).mapK(txr.trans)

  private final class Impl[T: Write](schema: Schema[T])(implicit
    lh: LogHandler
  ) extends Persist[T, ConnectionIO] {

    def persist(inputs: NonEmptyList[T]): ConnectionIO[Int] =
      schema.insertNoConflict.updateMany(inputs)
  }
}
