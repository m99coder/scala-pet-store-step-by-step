package io.m99.petstore

import cats.effect._
import cats.syntax.functor._
import doobie.util.ExecutionContexts
import io.circe.config.parser
import io.m99.petstore.config.{DatabaseConfig, PetStoreConfig}
import io.m99.petstore.domain.pets.{PetService, PetValidationInterpreter}
import io.m99.petstore.infrastructure.endpoint.PetEndpoints
import io.m99.petstore.infrastructure.repository.doobie.DoobiePetRepositoryInterpreter
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}

object Server extends IOApp {

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf             <- Resource.liftF(parser.decodePathF[F, PetStoreConfig]("petstore"))
      fixedThreadPool  <- ExecutionContexts.fixedThreadPool[F](conf.database.connections.poolSize)
      cachedThreadPool <- ExecutionContexts.cachedThreadPool[F]
      transactor       <- DatabaseConfig.transactor(conf.database, fixedThreadPool, cachedThreadPool)
      petRepository = DoobiePetRepositoryInterpreter[F](transactor)
      petValidation = PetValidationInterpreter[F](petRepository)
      petService    = PetService[F](petRepository, petValidation)
      services      = PetEndpoints.endpoints[F](petService)
      httpApp       = Router("/" -> services).orNotFound
      _ <- Resource.liftF(DatabaseConfig.initializeDb(conf.database))
      server <- BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] =
    createServer.use(_ => IO.never).as(ExitCode.Success)
}
