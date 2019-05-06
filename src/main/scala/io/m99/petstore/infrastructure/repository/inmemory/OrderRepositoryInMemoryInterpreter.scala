package io.m99.petstore.infrastructure.repository.inmemory

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.option._
import io.m99.petstore.domain.orders.{Order, OrderRepositoryAlgebra}

import scala.collection.concurrent.TrieMap
import scala.util.Random

class OrderRepositoryInMemoryInterpreter[F[_]: Applicative] extends OrderRepositoryAlgebra[F] {
  private val cache  = new TrieMap[Long, Order]()
  private val random = new Random()

  def create(order: Order): F[Order] = {
    val id     = random.nextLong
    val toSave = order.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  def get(orderId: Long): F[Option[Order]] = cache.get(orderId).pure[F]

  def delete(orderId: Long): F[Option[Order]] = cache.remove(orderId).pure[F]
}

object OrderRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new OrderRepositoryInMemoryInterpreter[F]()
}
