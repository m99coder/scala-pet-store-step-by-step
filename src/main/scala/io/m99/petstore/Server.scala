package io.m99.petstore

import cats.effect._
import cats.syntax.functor._
import io.circe.config.parser
import io.m99.petstore.config.PetStoreConfig
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Server => H4Server}

object Server extends IOApp {

  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.liftF(parser.decodePathF[F, PetStoreConfig]("petstore"))
      server <- BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] =
    createServer.use(_ => IO.never).as(ExitCode.Success)
}
