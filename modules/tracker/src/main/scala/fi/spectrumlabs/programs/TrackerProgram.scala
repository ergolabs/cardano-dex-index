package fi.spectrumlabs.programs

import cats.effect.Timer
import cats.syntax.foldable._
import cats.{Defer, Foldable, Functor, FunctorFilter, Monad, SemigroupK}
import fi.spectrumlabs.config.TrackerConfig
import fi.spectrumlabs.core.models.TxEvent
import fi.spectrumlabs.core.streaming.{Producer, Record}
import fi.spectrumlabs.repositories.TrackerCache
import fi.spectrumlabs.services.{Explorer, Filter}
import mouse.any._
import tofu.logging.{Logging, Logs}
import tofu.streams.{Compile, Evals, Pace, Temporal}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.combineK._
import tofu.syntax.streams.compile._
import tofu.syntax.streams.emits._
import tofu.syntax.streams.evals._
import tofu.syntax.streams.pace._
import tofu.syntax.streams.temporal._

trait TrackerProgram[S[_]] {
  def run: S[Unit]
}

object TrackerProgram {

  def create[
    S[_]: Monad: Evals[*[_], F]: FunctorFilter: Temporal[*[_], C]: Compile[*[_], F]: SemigroupK: Defer: Pace,
    F[_]: Monad: Timer,
    I[_]: Functor,
    C[_]: Foldable
  ](producer: Producer[String, TxEvent, S], config: TrackerConfig)(
    implicit
    cache: TrackerCache[F],
    explorer: Explorer[S, F],
    logs: Logs[I, F]
  ): I[TrackerProgram[S]] =
    logs.forService[TrackerProgram[S]].map(implicit __ => new Impl[S, F, C](producer, config))

  private final class Impl[
    S[_]: Monad: Evals[*[_], F]: FunctorFilter: Temporal[*[_], C]: Compile[*[_], F]: SemigroupK: Defer: Pace,
    F[_]: Monad: Logging,
    C[_]: Foldable
  ](producer: Producer[String, TxEvent, S], config: TrackerConfig)(
    implicit
    cache: TrackerCache[F],
    explorer: Explorer[S, F]
  ) extends TrackerProgram[S] {

    def run: S[Unit] =
      eval(cache.getLastOffset) >>= exec

    def exec(offset: Long): S[Unit] =
      eval(info"Current offset is: $offset. Going to perform next request.") >>
      explorer
        .streamTransactions(offset, config.limit)
        .groupWithin(config.batchSize, config.timeout)
        .evalMap { batch =>
          info"Received batch of ${batch.size} elems."
            .as {
              batch.toList.flatMap(TxEvent.fromExplorer).filter(Filter.txFilter).map(tx => Record(tx.hash.value, tx))
            }
            .flatMap { txn =>
              (emits[S](txn) |> producer.produce).drain
            }
            .flatMap { _ =>
              cache.setLastOffset(batch.size + offset + 1)
            }
        }
        .repeat
        .throttled(config.throttleRate)

  }
}
