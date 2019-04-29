package io.m99.petstore.domain.pets

import cats.data.EitherT
import io.m99.petstore.domain.PetNotFoundError

trait PetValidationAlgebra[F[_]] {
  def doesNotExist(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit]
}
