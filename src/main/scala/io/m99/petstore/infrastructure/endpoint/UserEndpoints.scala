package io.m99.petstore.infrastructure.endpoint

import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.circe.generic.auto._
import io.circe.syntax._
import io.m99.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}
import io.m99.petstore.domain.users.{User, UserService}
import io.m99.petstore.infrastructure.endpoint.Pagination.{
  OptionalLimitMatcher,
  OptionalOffsetMatcher
}
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UserEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]

  private def createUserEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "users" =>
        val action = for {
          user   <- req.as[User]
          result <- userService.createUser(user).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserAlreadyExistsError(existing)) =>
            Conflict(s"The user ${existing.userName} already exists")
        }
    }

  private def updateUserEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ PUT -> Root / "users" / name =>
        val action = for {
          user <- req.as[User]
          updated = user.copy(userName = name)
          result <- userService.update(updated).value
        } yield result

        action.flatMap {
          case Right(saved)            => Ok(saved.asJson)
          case Left(UserNotFoundError) => NotFound("The user not found")
        }
    }

  private def getUserEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "users" / userName =>
        userService.getUserByName(userName).value.flatMap {
          case Right(found)            => Ok(found.asJson)
          case Left(UserNotFoundError) => NotFound("The user not found")
        }
    }

  private def deleteUserEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "users" / userName =>
        for {
          _    <- userService.deleteByUserName(userName)
          resp <- NoContent()
        } yield resp
    }

  private def listUsersEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "users" :? OptionalLimitMatcher(limit) :? OptionalOffsetMatcher(offset) =>
        for {
          retrieved <- userService.list(limit.getOrElse(10), offset.getOrElse(0))
          resp      <- Ok(retrieved.asJson)
        } yield resp
    }

  def endpoints(userService: UserService[F]): HttpRoutes[F] =
    createUserEndpoint(userService) <+>
      updateUserEndpoint(userService) <+>
      getUserEndpoint(userService) <+>
      deleteUserEndpoint(userService) <+>
      listUsersEndpoint(userService)
}

object UserEndpoints {
  def endpoints[F[_]: Effect](userService: UserService[F]): HttpRoutes[F] =
    new UserEndpoints[F].endpoints(userService)
}
