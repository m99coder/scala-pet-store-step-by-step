package io.m99.petstore.domain.pets

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import io.m99.petstore.domain.{PetAlreadyExistsError, PetNotFoundError}

class PetValidationInterpreter[F[_]: Monad](repositoryAlgebra: PetRepositoryAlgebra[F])
    extends PetValidationAlgebra[F] {

  def doesAlreadyExist(pet: Pet): EitherT[F, PetAlreadyExistsError, Unit] =
    EitherT {
      repositoryAlgebra.findByNameAndCategory(pet.name, pet.category).map { matches =>
        if (matches.forall(possibleMatch => possibleMatch.bio != pet.bio)) {
          Right(())
        } else {
          Left(PetAlreadyExistsError(pet))
        }
      }
    }

  def doesNotExist(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit] =
    EitherT {
      petId match {
        case Some(id) =>
          repositoryAlgebra.get(id).map {
            case Some(_) => Right(())
            case _       => Left(PetNotFoundError)
          }
        case _ => Either.left[PetNotFoundError.type, Unit](PetNotFoundError).pure[F]
      }
    }
}

object PetValidationInterpreter {
  def apply[F[_]: Monad](repositoryAlgebra: PetRepositoryAlgebra[F]) =
    new PetValidationInterpreter[F](repositoryAlgebra)
}
