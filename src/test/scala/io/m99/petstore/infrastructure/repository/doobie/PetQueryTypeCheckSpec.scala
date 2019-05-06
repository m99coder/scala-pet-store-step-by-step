package io.m99.petstore.infrastructure.repository.doobie

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.applicative._
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import io.m99.petstore.PetStoreArbitraries
import org.scalatest.{FunSuite, Matchers}

class PetQueryTypeCheckSpec extends FunSuite with Matchers with IOChecker {
  override val transactor: Transactor[IO] = testTransactor

  import PetSQL._

  test(testName = "Type check pet queries") {
    PetStoreArbitraries.pet.arbitrary.sample.foreach { p =>
      check(selectByStatus(p.status.pure[NonEmptyList]))
      check(insert(p))
      p.id.foreach(id => check(update(p, id)))
    }
    check(selectTagLikeString("example".pure[NonEmptyList]))
    check(select(id = 1L))
    check(selectAll)
    check(delete(id = 1L))
    check(selectByNameAndCategory(name = "name", category = "category"))
  }
}
