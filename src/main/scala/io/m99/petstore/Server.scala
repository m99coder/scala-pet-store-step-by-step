package io.m99.petstore

import cats.effect._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import doobie.util.ExecutionContexts
import io.circe.config.parser
import io.m99.petstore.config.{DatabaseConfig, PetStoreConfig}
import io.m99.petstore.domain.orders.OrderService
import io.m99.petstore.domain.pets.{PetService, PetValidationInterpreter}
import io.m99.petstore.domain.users.{UserService, UserValidationInterpreter}
import io.m99.petstore.infrastructure.endpoint.{OrderEndpoints, PetEndpoints, UserEndpoints}
import io.m99.petstore.infrastructure.repository.doobie.{
  DoobieOrderRepositoryInterpreter,
  DoobiePetRepositoryInterpreter,
  DoobieUserRepositoryInterpreter
}
import org.http4s.syntax.kleisli._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf             <- Resource.liftF(parser.decodePathF[F, PetStoreConfig]("petstore"))
      fixedThreadPool  <- ExecutionContexts.fixedThreadPool[F](conf.database.connections.poolSize)
      cachedThreadPool <- ExecutionContexts.cachedThreadPool[F]
      transactor       <- DatabaseConfig.transactor(conf.database, fixedThreadPool, cachedThreadPool)
      petRepository   = DoobiePetRepositoryInterpreter[F](transactor)
      petValidation   = PetValidationInterpreter[F](petRepository)
      petService      = PetService[F](petRepository, petValidation)
      orderRepository = DoobieOrderRepositoryInterpreter[F](transactor)
      orderService    = OrderService[F](orderRepository)
      userRepository  = DoobieUserRepositoryInterpreter[F](transactor)
      userValidation  = UserValidationInterpreter[F](userRepository)
      userService     = UserService[F](userRepository, userValidation)
      services = PetEndpoints.endpoints[F](petService) <+>
        OrderEndpoints.endpoints[F](orderService) <+>
        UserEndpoints.endpoints[F, BCrypt](userService, BCrypt.syncPasswordHasher[F])
      httpApp = Router("/" -> services).orNotFound
      _ <- Resource.liftF(DatabaseConfig.initializeDb(conf.database))
      server <- BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] =
    createServer.use(_ => IO.never).as(ExitCode.Success)
}
