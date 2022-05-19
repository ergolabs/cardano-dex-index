package fi.spectrumlabs.db.writer.models

import cats.data.NonEmptyList
import fi.spectrumlabs.explorer.models.{OutRef, TxHash}
import fi.spectrumlabs.core.models.TxEvent
import fi.spectrumlabs.db.writer.classes.FromLedger

final case class Input(txHash: TxHash, txIndex: Long, outFef: OutRef, outIndex: Int, redeemerIndex: Option[Int])

object Input {

  implicit val fromLedger: FromLedger[TxEvent, NonEmptyList[Input]] = (in: TxEvent) =>
    in.inputs.map { i =>
      Input(
        in.hash,
        in.blockIndex,
        i.out.ref,
        i.out.index,
        i.redeemer.map(_.index)
      )
  }
}