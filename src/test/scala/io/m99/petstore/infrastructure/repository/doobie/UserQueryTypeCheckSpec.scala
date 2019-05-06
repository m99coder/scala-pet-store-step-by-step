package io.m99.petstore.infrastructure.repository.doobie

import cats.effect.IO
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import io.m99.petstore.PetStoreArbitraries
import org.scalatest.{FunSuite, Matchers}

class UserQueryTypeCheckSpec extends FunSuite with Matchers with IOChecker {
  override val transactor: Transactor[IO] = testTransactor

  import UserSQL._

  test(testName = "Type check user queries") {
    PetStoreArbitraries.user.arbitrary.sample.foreach { u =>
      check(insert(u))
      check(selectByUserName(u.userName))
      u.id.foreach(id => check(update(u, id)))
    }
    check(selectAll)
    check(select(userId = 1L))
    check(delete(userId = 1L))
  }
}
