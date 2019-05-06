package io.m99.petstore.infrastructure.repository.inmemory

import cats.Applicative
import cats.instances.option._
import cats.syntax.applicative._
import cats.syntax.option._
import cats.syntax.traverse._
import io.m99.petstore.domain.users.{User, UserRepositoryAlgebra}

import scala.collection.concurrent.TrieMap
import scala.util.Random

class UserRepositoryInMemoryInterpreter[F[_]: Applicative] extends UserRepositoryAlgebra[F] {
  private val cache  = new TrieMap[Long, User]()
  private val random = new Random()

  def create(user: User): F[User] = {
    val id     = random.nextLong
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  def update(user: User): F[Option[User]] =
    user.id.traverse { id =>
      cache.update(id, user)
      user.pure[F]
    }

  def get(userId: Long): F[Option[User]] = cache.get(userId).pure[F]

  def delete(userId: Long): F[Option[User]] = cache.remove(userId).pure[F]

  def findByUserName(userName: String): F[Option[User]] =
    cache.values.find(u => u.userName == userName).pure[F]

  def list(limit: Int, offset: Int): F[List[User]] =
    cache.values.toList.sortBy(_.lastName).slice(offset, offset + limit).pure[F]

  def deleteByUserName(userName: String): F[Option[User]] = {
    val deleted = for {
      user    <- cache.values.find(u => u.userName == userName)
      removed <- cache.remove(user.id.get)
    } yield removed
    deleted.pure[F]
  }
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new UserRepositoryInMemoryInterpreter[F]()
}
