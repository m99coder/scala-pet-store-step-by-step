package io.m99.petstore.domain.pets

import cats.data.NonEmptyList

trait PetRepositoryAlgebra[F[_]] {
  def create(pet: Pet): F[Pet]
  def update(pet: Pet): F[Option[Pet]]
  def get(id: Long): F[Option[Pet]]
  def delete(id: Long): F[Option[Pet]]

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]]
  def list(limit: Int, offset: Int): F[List[Pet]]
  def findByStatus(status: NonEmptyList[PetStatus]): F[List[Pet]]
  def findByTag(tags: NonEmptyList[String]): F[List[Pet]]
}
