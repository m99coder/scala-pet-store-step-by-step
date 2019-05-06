package io.m99.petstore.infrastructure.endpoint

import cats.effect.IO
import io.circe.generic.auto._
import io.m99.petstore.PetStoreArbitraries
import io.m99.petstore.domain.pets.{Pet, PetService, PetValidationInterpreter}
import io.m99.petstore.infrastructure.repository.inmemory.PetRepositoryInMemoryInterpreter
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.kleisli._
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class PetEndpointsSpec
    extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  implicit val petDecoder: EntityDecoder[IO, Pet] = jsonOf
  implicit val petEncoder: EntityEncoder[IO, Pet] = jsonEncoderOf

  test(testName = "create pet") {
    val petRepo        = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation  = PetValidationInterpreter[IO](petRepo)
    val petService     = PetService[IO](petRepo, petValidation)
    val petHttpService = PetEndpoints.endpoints[IO](petService).orNotFound

    forAll { pet: Pet =>
      (for {
        request  <- POST(pet, Uri.uri("/pets"))
        response <- petHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }
  }

  test(testName = "update pet") {
    val petRepo        = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation  = PetValidationInterpreter[IO](petRepo)
    val petService     = PetService[IO](petRepo, petValidation)
    val petHttpService = PetEndpoints.endpoints[IO](petService).orNotFound

    forAll { pet: Pet =>
      (for {
        createRequest  <- POST(pet, Uri.uri("/pets"))
        createResponse <- petHttpService.run(createRequest)
        createdPet     <- createResponse.as[Pet]
        petToUpdate = createdPet.copy(name = createdPet.name.reverse)
        updateRequest  <- PUT(petToUpdate, Uri.unsafeFromString(s"/pets/${petToUpdate.id.get}"))
        updateResponse <- petHttpService.run(updateRequest)
        updatedPet     <- updateResponse.as[Pet]
      } yield {
        updateResponse.status shouldEqual Ok
        updatedPet.name shouldEqual pet.name.reverse
        createdPet.id shouldEqual updatedPet.id
      }).unsafeRunSync
    }
  }
}
