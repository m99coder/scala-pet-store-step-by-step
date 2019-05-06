package io.m99.petstore

import java.time.Instant

import io.m99.petstore.domain.authentication.SignUpRequest
import io.m99.petstore.domain.orders.OrderStatus.{Approved, Delivered, Placed}
import io.m99.petstore.domain.orders.{Order, OrderStatus}
import io.m99.petstore.domain.pets.PetStatus.{Adopted, Available, Pending}
import io.m99.petstore.domain.pets.{Pet, PetStatus}
import io.m99.petstore.domain.users.User
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait PetStoreArbitraries {
  val userNameLength           = 26
  val userNameGen: Gen[String] = Gen.listOfN(userNameLength, Gen.alphaChar).map(_.mkString)

  implicit val instant: Arbitrary[Instant] = Arbitrary[Instant] {
    for {
      millis <- Gen.posNum[Long]
    } yield Instant.ofEpochMilli(millis)
  }

  implicit val orderStatus: Arbitrary[OrderStatus] = Arbitrary[OrderStatus] {
    Gen.oneOf(Approved, Delivered, Placed)
  }

  implicit val order: Arbitrary[Order] = Arbitrary[Order] {
    for {
      petId    <- Gen.posNum[Long]
      shipDate <- Gen.option(instant.arbitrary)
      status   <- arbitrary[OrderStatus]
      complete <- arbitrary[Boolean]
      id       <- Gen.option(Gen.posNum[Long])
    } yield Order(petId, shipDate, status, complete, id)
  }

  implicit val petStatus: Arbitrary[PetStatus] = Arbitrary[PetStatus] {
    Gen.oneOf(Available, Pending, Adopted)
  }

  implicit val pet: Arbitrary[Pet] = Arbitrary[Pet] {
    for {
      name     <- arbitrary[String]
      category <- arbitrary[String]
      bio      <- arbitrary[String]
      status   <- arbitrary[PetStatus]
      numTags  <- Gen.choose(min = 0, max = 10)
      tags     <- Gen.listOfN(numTags, Gen.alphaStr).map(_.toSet)
      photoUrls <- Gen
        .listOfN(numTags, Gen.alphaStr)
        .map(_.map(x => s"https://$x.com"))
        .map(_.toSet)
      id <- Gen.option(Gen.posNum[Long])
    } yield Pet(name, category, bio, status, tags, photoUrls, id)
  }

  implicit val user: Arbitrary[User] = Arbitrary[User] {
    for {
      userName  <- userNameGen
      firstName <- arbitrary[String]
      lastName  <- arbitrary[String]
      email     <- arbitrary[String]
      password  <- arbitrary[String]
      id        <- Gen.option(Gen.posNum[Long])
    } yield User(userName, firstName, lastName, email, password, id)
  }

  implicit val userSignUp: Arbitrary[SignUpRequest] = Arbitrary[SignUpRequest] {
    for {
      userName  <- userNameGen
      firstName <- arbitrary[String]
      lastName  <- arbitrary[String]
      email     <- arbitrary[String]
      password  <- arbitrary[String]
    } yield SignUpRequest(userName, firstName, lastName, email, password)
  }
}

object PetStoreArbitraries extends PetStoreArbitraries
