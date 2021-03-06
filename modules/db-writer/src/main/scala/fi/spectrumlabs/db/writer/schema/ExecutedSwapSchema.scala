package fi.spectrumlabs.db.writer.schema

import fi.spectrumlabs.db.writer.models.db.ExecutedSwap

class ExecutedSwapSchema extends Schema[ExecutedSwap] {
  val tableName: String = "executed_swap"

  val fields: List[String] = List(
    "base",
    "quote",
    "pool_nft",
    "ex_fee_per_token_num",
    "ex_fee_per_token_den",
    "reward_pkh",
    "stake_pkh",
    "base_amount",
    "actual_quote",
    "min_quote_amount",
    "order_input_id",
    "user_output_id",
    "pool_input_Id",
    "pool_output_Id",
    "timestamp"
  )
}
