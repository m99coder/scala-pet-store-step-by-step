package io.m99.petstore.domain.pets

import cats.data.EitherT
import io.m99.petstore.domain.{PetAlreadyExistsError, PetNotFoundError}

trait PetValidationAlgebra[F[_]] {
  def doesAlreadyExist(pet: Pet): EitherT[F, PetAlreadyExistsError, Unit]
  def doesNotExist(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit]
}
