package fi.spectrumlabs.db.writer.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import fi.spectrumlabs.core.streaming.config.{ConsumerConfig, KafkaConfig}
import tofu.WithContext
import tofu.logging.derivation.loggable
import tofu.optics.macros.ClassyOptics

@ClassyOptics
@derive(loggable, pureconfigReader)
final case class ConfigBundle(
  pg: PgConfig,
  txConsumer: ConsumerConfig,
  executedOpsConsumer: ConsumerConfig,
  poolsConsumer: ConsumerConfig,
  kafka: KafkaConfig,
  writer: WriterConfig
)

object ConfigBundle extends WithContext.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle]
