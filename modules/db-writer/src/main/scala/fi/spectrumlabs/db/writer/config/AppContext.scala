package fi.spectrumlabs.db.writer.config

import derevo.derive
import tofu.WithContext
import tofu.logging.derivation.{hidden, loggable}
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
@derive(loggable)
final case class AppContext(
  @promote config: ConfigBundle,
)

object AppContext extends WithContext.Companion[AppContext] {

  def init(configs: ConfigBundle): AppContext =
    AppContext(configs)
}
