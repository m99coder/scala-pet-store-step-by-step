package io.m99.petstore.infrastructure.endpoint

import cats.effect.IO
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.m99.petstore.PetStoreArbitraries
import io.m99.petstore.domain.orders.{Order, OrderService, OrderStatus}
import io.m99.petstore.infrastructure.repository.inmemory.OrderRepositoryInMemoryInterpreter
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.kleisli._
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class OrderEndpointsSpec
    extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  implicit val statusDecoder: EntityDecoder[IO, OrderStatus] = jsonOf
  implicit val statusEncoder: EntityEncoder[IO, OrderStatus] = jsonEncoderOf

  implicit val orderEnc: Encoder[Order]               = deriveEncoder
  implicit val orderEncoder: EntityEncoder[IO, Order] = jsonEncoderOf

  test(testName = "place order") {
    val orderService     = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
    val orderHttpService = OrderEndpoints.endpoints[IO](orderService).orNotFound

    forAll { order: Order =>
      (for {
        request  <- POST(order, Uri.uri("/orders"))
        response <- orderHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }
  }
}
