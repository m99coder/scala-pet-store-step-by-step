package io.m99.petstore.domain.users

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.either._
import io.m99.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserValidationInterpreter[F[_]: Monad](userRepositoryAlgebra: UserRepositoryAlgebra[F])
    extends UserValidationAlgebra[F] {

  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] =
    EitherT {
      userRepositoryAlgebra.findByUserName(user.userName).map {
        case None    => Right(())
        case Some(_) => Left(UserAlreadyExistsError(user))
      }
    }

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    EitherT {
      userId
        .map { id =>
          userRepositoryAlgebra.get(id).map {
            case Some(_) => Right(())
            case _       => Left(UserNotFoundError)
          }
        }
        .getOrElse(
          Either.left[UserNotFoundError.type, Unit](UserNotFoundError).pure[F]
        )
    }
}

object UserValidationInterpreter {
  def apply[F[_]: Monad](repositoryAlgebra: UserRepositoryAlgebra[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](repositoryAlgebra)
}
