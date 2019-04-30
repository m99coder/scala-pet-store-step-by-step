package io.m99.petstore.domain.orders

trait OrderRepositoryAlgebra[F[_]] {
  def create(order: Order): F[Order]
  def get(orderId: Long): F[Option[Order]]
  def delete(orderId: Long): F[Option[Order]]
}
