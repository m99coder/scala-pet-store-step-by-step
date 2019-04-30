package io.m99.petstore.domain.users

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import io.m99.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]: Monad](userRepositoryAlgebra: UserRepositoryAlgebra[F],
                               validationAlgebra: UserValidationAlgebra[F]) {

  def createUser(user: User): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _     <- validationAlgebra.doesNotExist(user)
      saved <- EitherT.liftF(userRepositoryAlgebra.create(user))
    } yield saved

  def getUser(userId: Long): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(userRepositoryAlgebra.get(userId), UserNotFoundError)

  def getUserByName(userName: String): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(userRepositoryAlgebra.findByUserName(userName), UserNotFoundError)

  def deleteUser(userId: Long): F[Unit] = userRepositoryAlgebra.delete(userId).as(())

  def deleteByUserName(userName: String): F[Unit] =
    userRepositoryAlgebra.deleteByUserName(userName).as(())

  def update(user: User): EitherT[F, UserNotFoundError.type, User] =
    for {
      _     <- validationAlgebra.exists(user.id)
      saved <- EitherT.fromOptionF(userRepositoryAlgebra.update(user), UserNotFoundError)
    } yield saved

  def list(limit: Int, offset: Int): F[List[User]] = userRepositoryAlgebra.list(limit, offset)
}

object UserService {
  def apply[F[_]: Monad](repositoryAlgebra: UserRepositoryAlgebra[F],
                         validationAlgebra: UserValidationAlgebra[F]): UserService[F] =
    new UserService[F](repositoryAlgebra, validationAlgebra)
}
