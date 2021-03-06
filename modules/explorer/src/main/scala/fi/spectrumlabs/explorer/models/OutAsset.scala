package fi.spectrumlabs.explorer.models

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(encoder, decoder)
final case class OutAsset(
  policyId: PolicyId,
  name: Asset32,
  quantity: BigInt,
  jsQuantity: String
)
