package io.m99.petstore.infrastructure.repository

import cats.effect.{Async, ContextShift, Effect, IO}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.m99.petstore.config.{DatabaseConfig, PetStoreConfig}
import _root_.doobie.Transactor
import io.circe.config.parser

import scala.concurrent.ExecutionContext

package object doobie {

  def getTransactor[F[_]: Async: ContextShift](databaseConfig: DatabaseConfig): Transactor[F] =
    Transactor.fromDriverManager[F](databaseConfig.driver,
                                    databaseConfig.url,
                                    databaseConfig.user,
                                    databaseConfig.password)

  def initializedTransactor[F[_]: Effect: Async: ContextShift]: F[Transactor[F]] =
    for {
      petStoreConfig <- parser.decodePathF[F, PetStoreConfig](path = "petstore")
      _              <- DatabaseConfig.initializeDb(petStoreConfig.database)
    } yield getTransactor(petStoreConfig.database)

  lazy val testExecutionContext: ExecutionContext      = ExecutionContext.Implicits.global
  implicit lazy val testContextShift: ContextShift[IO] = IO.contextShift(testExecutionContext)
  lazy val testTransactor: Transactor[IO]              = initializedTransactor[IO].unsafeRunSync
}
