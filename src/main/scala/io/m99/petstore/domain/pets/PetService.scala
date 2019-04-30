package io.m99.petstore.domain.pets

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.functor._
import io.m99.petstore.domain.{PetAlreadyExistsError, PetNotFoundError}

class PetService[F[_]](repositoryAlgebra: PetRepositoryAlgebra[F],
                       validationAlgebra: PetValidationAlgebra[F]) {
  def create(pet: Pet)(implicit M: Monad[F]): EitherT[F, PetAlreadyExistsError, Pet] =
    for {
      _     <- validationAlgebra.doesAlreadyExist(pet)
      saved <- EitherT.liftF(repositoryAlgebra.create(pet))
    } yield saved
  def update(pet: Pet)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    for {
      _     <- validationAlgebra.doesNotExist(pet.id)
      saved <- EitherT.fromOptionF(repositoryAlgebra.update(pet), PetNotFoundError)
    } yield saved
  def get(id: Long)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    EitherT.fromOptionF(repositoryAlgebra.get(id), PetNotFoundError)
  def delete(id: Long)(implicit M: Monad[F]): F[Unit] = repositoryAlgebra.delete(id).as(())

  def list(limit: Int, offset: Int): F[List[Pet]] =
    repositoryAlgebra.list(limit, offset)
  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    repositoryAlgebra.findByStatus(statuses)
  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    repositoryAlgebra.findByTag(tags)
}

object PetService {
  def apply[F[_]: Monad](repositoryAlgebra: PetRepositoryAlgebra[F],
                         validationAlgebra: PetValidationAlgebra[F]) =
    new PetService[F](repositoryAlgebra, validationAlgebra)
}
