package io.m99.petstore.domain.pets

trait PetRepositoryAlgebra[F[_]] {
  def create(pet: Pet): F[Pet]
  def update(pet: Pet): F[Option[Pet]]
  def get(id: Long): F[Option[Pet]]
  def delete(id: Long): F[Option[Pet]]
}
