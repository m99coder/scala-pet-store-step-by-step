package io.m99.petstore.infrastructure.repository.doobie

import cats.effect.IO
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import io.m99.petstore.PetStoreArbitraries
import org.scalatest.{FunSuite, Matchers}

class OrderQueryTypeCheckSpec extends FunSuite with Matchers with IOChecker {
  override val transactor: Transactor[IO] = testTransactor

  import OrderSQL._

  test(testName = "Type check order queries") {
    PetStoreArbitraries.order.arbitrary.sample.foreach { o =>
      check(insert(o))
    }
    check(delete(orderId = 1L))
    check(select(orderId = 1L))
  }
}
