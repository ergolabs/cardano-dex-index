package fi.spectrumlabs.markets.api

import derevo.derive
import io.estatico.newtype.macros.newtype
import tofu.{WithContext, WithLocal}
import tofu.logging.derivation.loggable

package object context {
  @derive(loggable)
  @newtype final case class TraceId(value: String)

  object TraceId {
    type Local[F[_]] = WithLocal[F, TraceId]
    type Has[F[_]]   = WithContext[F, TraceId]

    def fromString(s: String): TraceId = apply(s)
  }
}
