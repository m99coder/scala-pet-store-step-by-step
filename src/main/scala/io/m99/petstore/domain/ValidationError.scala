package io.m99.petstore.domain

sealed trait ValidationError extends Product with Serializable

case object PetNotFoundError extends ValidationError
