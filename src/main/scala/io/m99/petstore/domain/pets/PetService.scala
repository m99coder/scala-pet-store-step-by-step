package io.m99.petstore.domain.pets

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import io.m99.petstore.domain.PetNotFoundError

class PetService[F[_]](repositoryAlgebra: PetRepositoryAlgebra[F],
                       validationAlgebra: PetValidationAlgebra[F]) {
  def create(pet: Pet): F[Pet] = repositoryAlgebra.create(pet)
  def update(pet: Pet)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    for {
      _     <- validationAlgebra.doesNotExist(pet.id)
      saved <- EitherT.fromOptionF(repositoryAlgebra.update(pet), PetNotFoundError)
    } yield saved
  def get(id: Long)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    EitherT.fromOptionF(repositoryAlgebra.get(id), PetNotFoundError)
  def delete(id: Long)(implicit M: Monad[F]): F[Unit] = repositoryAlgebra.delete(id).as(())
}

object PetService {
  def apply[F[_]: Monad](repositoryAlgebra: PetRepositoryAlgebra[F],
                         validationAlgebra: PetValidationAlgebra[F]) =
    new PetService[F](repositoryAlgebra, validationAlgebra)
}
