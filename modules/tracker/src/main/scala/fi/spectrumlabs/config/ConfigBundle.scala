package fi.spectrumlabs.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import tofu.logging.derivation.loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(loggable, pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote explorer: ExplorerConfig,
  @promote tracker: TrackerConfig,
  @promote redis: RedisConfig,
  @promote producer: ProducerConfig,
  @promote kafka: KafkaConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle]
