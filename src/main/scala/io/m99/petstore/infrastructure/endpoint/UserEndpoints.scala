package io.m99.petstore.infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Effect
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.circe.generic.auto._
import io.circe.syntax._
import io.m99.petstore.domain.authentication.{LoginRequest, SignUpRequest}
import io.m99.petstore.domain.users.{User, UserService}
import io.m99.petstore.domain.{
  UserAlreadyExistsError,
  UserAuthenticationFailedError,
  UserNotFoundError
}
import io.m99.petstore.infrastructure.endpoint.Pagination.{
  OptionalLimitMatcher,
  OptionalOffsetMatcher
}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.common.Verified
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class UserEndpoints[F[_]: Effect, A] extends Http4sDsl[F] {
  implicit val userDecoder: EntityDecoder[F, User]                   = jsonOf[F, User]
  implicit val loginRequestDecoder: EntityDecoder[F, LoginRequest]   = jsonOf[F, LoginRequest]
  implicit val signUpRequestDecoder: EntityDecoder[F, SignUpRequest] = jsonOf[F, SignUpRequest]

  private def createUserEndpoint(userService: UserService[F],
                                 cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "users" =>
        val action = for {
          signUp <- req.as[SignUpRequest]
          hash   <- cryptService.hashpw(signUp.password)
          user   <- signUp.asUser(hash).pure[F]
          result <- userService.createUser(user).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserAlreadyExistsError(existing)) =>
            Conflict(s"The user with user name ${existing.userName} already exists")
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

  private def loginEndpoint(userService: UserService[F],
                            cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        val action: EitherT[F, UserAuthenticationFailedError, User] =
          for {
            login <- EitherT.liftF(req.as[LoginRequest])
            name = login.userName
            user <- userService
              .getUserByName(name)
              .leftMap(_ => UserAuthenticationFailedError(name))
            checkResult <- EitherT.liftF(
              cryptService.checkpw(login.password, PasswordHash[A](user.password)))
            resp <- if (checkResult == Verified)
              EitherT.rightT[F, UserAuthenticationFailedError](user)
            else EitherT.leftT[F, User](UserAuthenticationFailedError(name))
          } yield resp

        action.value.flatMap {
          case Right(user) => Ok(user.asJson)
          case Left(UserAuthenticationFailedError(name)) =>
            BadRequest(s"User $name is unauthorized")
        }
    }

  def endpoints(userService: UserService[F], cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    createUserEndpoint(userService, cryptService) <+>
      updateUserEndpoint(userService) <+>
      getUserEndpoint(userService) <+>
      deleteUserEndpoint(userService) <+>
      listUsersEndpoint(userService) <+>
      loginEndpoint(userService, cryptService)
}

object UserEndpoints {
  def endpoints[F[_]: Effect, A](userService: UserService[F],
                                 cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    new UserEndpoints[F, A].endpoints(userService, cryptService)
}
