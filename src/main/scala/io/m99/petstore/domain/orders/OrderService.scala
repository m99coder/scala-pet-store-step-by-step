package io.m99.petstore.domain.orders

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import io.m99.petstore.domain.OrderNotFoundError

class OrderService[F[_]](orderRepositoryAlgebra: OrderRepositoryAlgebra[F]) {
  def placeOrder(order: Order): F[Order] = orderRepositoryAlgebra.create(order)
  def get(id: Long)(implicit M: Monad[F]): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepositoryAlgebra.get(id), OrderNotFoundError)
  def delete(id: Long)(implicit M: Monad[F]): F[Unit] = orderRepositoryAlgebra.delete(id).as(())
}

object OrderService {
  def apply[F[_]](orderRepositoryAlgebra: OrderRepositoryAlgebra[F]): OrderService[F] =
    new OrderService(orderRepositoryAlgebra)
}
