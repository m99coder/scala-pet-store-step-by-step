package io.m99.petstore.infrastructure.endpoint

import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.circe.generic.auto._
import io.circe.syntax._
import io.m99.petstore.domain.OrderNotFoundError
import io.m99.petstore.domain.orders.{Order, OrderService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}

class OrderEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  implicit val orderDecoder: EntityDecoder[F, Order] = jsonOf[F, Order]

  private def createOrderEndpoint(orderService: OrderService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "orders" =>
        for {
          order <- req.as[Order]
          saved <- orderService.placeOrder(order)
          resp  <- Ok(saved.asJson)
        } yield resp
    }

  private def getOrderEndpoint(orderService: OrderService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "orders" / LongVar(id) =>
        orderService.get(id).value.flatMap {
          case Right(found)             => Ok(found.asJson)
          case Left(OrderNotFoundError) => NotFound("The order was not found")
        }
    }

  private def deleteOrderEndpoint(orderService: OrderService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "orders" / LongVar(id) =>
        for {
          _    <- orderService.delete(id)
          resp <- NoContent()
        } yield resp
    }

  def endpoints(orderService: OrderService[F]): HttpRoutes[F] =
    createOrderEndpoint(orderService) <+>
      getOrderEndpoint(orderService) <+>
      deleteOrderEndpoint(orderService)
}

object OrderEndpoints {
  def endpoints[F[_]: Effect](orderService: OrderService[F]): HttpRoutes[F] =
    new OrderEndpoints[F].endpoints(orderService)
}
