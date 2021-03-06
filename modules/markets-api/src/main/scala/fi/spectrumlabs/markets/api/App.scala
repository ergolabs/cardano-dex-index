package fi.spectrumlabs.markets.api

import cats.effect.{Blocker, Resource}
import dev.profunktor.redis4cats.RedisCommands
import fi.spectrumlabs.core.EnvApp
import fi.spectrumlabs.core.pg.{doobieLogging, PostgresTransactor}
import fi.spectrumlabs.core.redis.codecs.stringCodec
import fi.spectrumlabs.core.redis.mkRedis
import fi.spectrumlabs.markets.api.configs.ConfigBundle
import fi.spectrumlabs.markets.api.context.AppContext
import fi.spectrumlabs.markets.api.repositories.repos.{PoolsRepo, RatesRepo}
import fi.spectrumlabs.markets.api.services.AnalyticsService
import fi.spectrumlabs.markets.api.v1.HttpServer
import org.http4s.server.Server
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.lift.{IsoK, Unlift}
import tofu.logging.Logs
import tofu.logging.derivation.loggable.generate
import zio.interop.catz._
import zio.{ExitCode, URIO, ZIO}

object App extends EnvApp[AppContext] {

  implicit val serverOptions: Http4sServerOptions[RunF, RunF] = Http4sServerOptions.default[RunF, RunF]

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    init(args.headOption).use(_ => ZIO.never).orDie

  def init(configPathOpt: Option[String]): Resource[InitF, Server] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      ctx                                = AppContext.init(configs)
      implicit0(ul: Unlift[RunF, InitF]) = Unlift.byIso(IsoK.byFunK(wr.runContextK(ctx))(wr.liftF))
      trans <- PostgresTransactor.make[InitF]("markets-api-db-pool", configs.pg)
      implicit0(xa: Txr.Continuational[RunF]) = Txr.continuational[RunF](trans.mapK(wr.liftF))
      implicit0(elh: EmbeddableLogHandler[xa.DB]) <- Resource.eval(
                                                       doobieLogging.makeEmbeddableHandler[InitF, RunF, xa.DB](
                                                         "markets-api-db-logging"
                                                       )
                                                     )
      implicit0(logsDb: Logs[InitF, xa.DB]) = Logs.sync[InitF, xa.DB]
      implicit0(redis: RedisCommands[RunF, String, String]) <- mkRedis[String, String, InitF, RunF](
                                                                 configs.redis,
                                                                 stringCodec
                                                               )
      implicit0(poolsRepo: PoolsRepo[RunF]) <- Resource.eval(PoolsRepo.create[InitF, xa.DB, RunF])
      implicit0(ratesRepo: RatesRepo[RunF]) <- Resource.eval(RatesRepo.create[InitF, RunF])
      implicit0(service: AnalyticsService[RunF]) <- Resource.eval(
                                                      AnalyticsService.create[InitF, RunF](configs.marketsApi)
                                                    )
      server <- HttpServer.make[InitF, RunF](configs.http, runtime.platform.executor.asEC)
    } yield server
}
