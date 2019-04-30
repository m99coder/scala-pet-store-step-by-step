package io.m99.petstore.domain

import io.m99.petstore.domain.pets.Pet

sealed trait ValidationError extends Product with Serializable

case class PetAlreadyExistsError(pet: Pet) extends ValidationError
case object PetNotFoundError               extends ValidationError
