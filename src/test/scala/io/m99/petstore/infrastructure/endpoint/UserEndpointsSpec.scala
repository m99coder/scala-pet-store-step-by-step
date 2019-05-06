package io.m99.petstore.infrastructure.endpoint

import cats.effect.IO
import io.circe.generic.auto._
import io.m99.petstore.PetStoreArbitraries
import io.m99.petstore.domain.authentication.SignUpRequest
import io.m99.petstore.domain.users.{User, UserService, UserValidationInterpreter}
import io.m99.petstore.infrastructure.repository.inmemory.UserRepositoryInMemoryInterpreter
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.kleisli._
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.passwordhashers.jca.BCrypt

class UserEndpointsSpec
    extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf
  implicit val userEncoder: EntityEncoder[IO, User] = jsonEncoderOf

  implicit val signUpRequestDecoder: EntityDecoder[IO, SignUpRequest] = jsonOf
  implicit val signUpRequestEncoder: EntityEncoder[IO, SignUpRequest] = jsonEncoderOf

  test(testName = "create user") {
    val userRepo       = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService    = UserService[IO](userRepo, userValidation)
    val userHttpService =
      UserEndpoints.endpoints[IO, BCrypt](userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignUp: SignUpRequest =>
      (for {
        request  <- POST(userSignUp, Uri.uri("/users"))
        response <- userHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }
  }

  test(testName = "update user") {
    val userRepo       = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService    = UserService[IO](userRepo, userValidation)
    val userHttpService =
      UserEndpoints.endpoints[IO, BCrypt](userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignUp: SignUpRequest =>
      (for {
        createRequest  <- POST(userSignUp, Uri.uri("/users"))
        createResponse <- userHttpService.run(createRequest)
        createdUser    <- createResponse.as[User]
        userToUpdate = createdUser.copy(lastName = createdUser.lastName.reverse)
        updateRequest  <- PUT(userToUpdate, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        updateResponse <- userHttpService.run(updateRequest)
        updatedUser    <- updateResponse.as[User]
      } yield {
        createResponse.status shouldEqual Ok
        updateResponse.status shouldEqual Ok
        updatedUser.lastName shouldEqual createdUser.lastName.reverse
        createdUser.id shouldEqual updatedUser.id
      }).unsafeRunSync
    }
  }

  test(testName = "get user by userName") {
    val userRepo       = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService    = UserService[IO](userRepo, userValidation)
    val userHttpService =
      UserEndpoints.endpoints[IO, BCrypt](userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignUp: SignUpRequest =>
      (for {
        createRequest  <- POST(userSignUp, Uri.uri("/users"))
        createResponse <- userHttpService.run(createRequest)
        createdUser    <- createResponse.as[User]
        getRequest     <- GET(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getResponse    <- userHttpService.run(getRequest)
        getUser        <- getResponse.as[User]
      } yield {
        createResponse.status shouldEqual Ok
        getResponse.status shouldEqual Ok
        createdUser.userName shouldEqual getUser.userName
      }).unsafeRunSync
    }
  }

  test(testName = "delete user by userName") {
    val userRepo       = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService    = UserService[IO](userRepo, userValidation)
    val userHttpService =
      UserEndpoints.endpoints[IO, BCrypt](userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignUp: SignUpRequest =>
      (for {
        createRequest  <- POST(userSignUp, Uri.uri("/users"))
        createResponse <- userHttpService.run(createRequest)
        createdUser    <- createResponse.as[User]
        deleteRequest  <- DELETE(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        deleteResponse <- userHttpService.run(deleteRequest)
        getRequest     <- GET(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getResponse    <- userHttpService.run(getRequest)
      } yield {
        createResponse.status shouldEqual Ok
        deleteResponse.status shouldEqual NoContent
        getResponse.status shouldEqual NotFound
      }).unsafeRunSync
    }
  }
}
