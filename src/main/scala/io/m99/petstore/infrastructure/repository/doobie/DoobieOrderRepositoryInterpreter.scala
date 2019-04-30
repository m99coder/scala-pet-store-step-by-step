package io.m99.petstore.infrastructure.repository.doobie

import java.time.Instant

import cats.Monad
import cats.data.OptionT
import cats.syntax.functor._
import cats.syntax.option._
import doobie.implicits._
import doobie.util.Meta
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import io.m99.petstore.domain.orders.{Order, OrderRepositoryAlgebra, OrderStatus}

private object OrderSQL {
  implicit val StatusMeta: Meta[OrderStatus] =
    Meta[String].imap(OrderStatus.withName)(_.entryName)
  implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].imap(_.toInstant)(java.sql.Timestamp.from _)

  def select(orderId: Long): Query0[Order] =
    sql"""SELECT pet_id, ship_date, status, complete, id
          FROM orders
          WHERE ID = $orderId""".query[Order]

  def insert(order: Order): Update0 =
    sql"""INSERT INTO orders (pet_id, ship_date, status, complete)
          VALUES (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})
       """.update

  def delete(orderId: Long): Update0 =
    sql"""DELETE FROM orders
          WHERE id = $orderId""".update
}

class DoobieOrderRepositoryInterpreter[F[_]: Monad](val transactor: Transactor[F])
    extends OrderRepositoryAlgebra[F] {
  import OrderSQL._

  def create(order: Order): F[Order] =
    insert(order)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => order.copy(id = id.some))
      .transact(transactor)

  def get(orderId: Long): F[Option[Order]] = select(orderId).option.transact(transactor)

  def delete(orderId: Long): F[Option[Order]] =
    OptionT(get(orderId))
      .semiflatMap(order => OrderSQL.delete(orderId).run.transact(transactor).as(order))
      .value
}

object DoobieOrderRepositoryInterpreter {
  def apply[F[_]: Monad](transactor: Transactor[F]): DoobieOrderRepositoryInterpreter[F] =
    new DoobieOrderRepositoryInterpreter(transactor)
}
