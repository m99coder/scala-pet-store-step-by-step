package io.m99.petstore.infrastructure.repository.inmemory

import cats.Applicative
import cats.data.NonEmptyList
import cats.instances.option._
import cats.syntax.applicative._
import cats.syntax.option._
import cats.syntax.traverse._
import io.m99.petstore.domain.pets.{Pet, PetRepositoryAlgebra, PetStatus}

import scala.collection.concurrent.TrieMap
import scala.util.Random

class PetRepositoryInMemoryInterpreter[F[_]: Applicative] extends PetRepositoryAlgebra[F] {
  private val cache  = new TrieMap[Long, Pet]()
  private val random = new Random()

  def create(pet: Pet): F[Pet] = {
    val id     = random.nextLong
    val toSave = pet.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  def update(pet: Pet): F[Option[Pet]] =
    pet.id.traverse { id =>
      cache.update(id, pet)
      pet.pure[F]
    }

  def get(id: Long): F[Option[Pet]] = cache.get(id).pure[F]

  def delete(id: Long): F[Option[Pet]] = cache.remove(id).pure[F]

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    cache.values
      .filter(p => p.name == name && p.category == category)
      .toSet
      .pure[F]

  def list(limit: Int, offset: Int): F[List[Pet]] =
    cache.values.toList.sortBy(_.name).slice(offset, offset + limit).pure[F]

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    cache.values.filter(p => statuses.exists(_ == p.status)).toList.pure[F]

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    cache.values
      .filter(p => p.tags.exists(pt => tags.exists(_ == pt)))
      .toList
      .pure[F]
}

object PetRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new PetRepositoryInMemoryInterpreter[F]()
}
