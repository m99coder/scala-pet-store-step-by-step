package io.m99.petstore.config

import cats.effect.{Async, ContextShift, Resource}
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

case class DatabaseConnectionsConfig(poolSize: Int)
case class DatabaseConfig(url: String,
                          driver: String,
                          user: String,
                          password: String,
                          connections: DatabaseConnectionsConfig)

object DatabaseConfig {
  def transactor[F[_]: Async: ContextShift](
      config: DatabaseConfig,
      fixedThreadPool: ExecutionContext,
      cachedThreadPool: ExecutionContext): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](config.driver,
                                            config.url,
                                            config.user,
                                            config.password,
                                            fixedThreadPool,
                                            cachedThreadPool)
}
