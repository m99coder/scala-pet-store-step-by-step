package io.m99.petstore.infrastructure.endpoint

import cats.data.NonEmptyList
import cats.data.Validated.Valid
import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.circe.generic.auto._
import io.circe.syntax._
import io.m99.petstore.domain.pets.{Pet, PetService, PetStatus}
import io.m99.petstore.domain.{PetAlreadyExistsError, PetNotFoundError}
import io.m99.petstore.infrastructure.endpoint.Pagination.{
  OptionalLimitMatcher,
  OptionalOffsetMatcher
}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder}

class PetEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  implicit val petDecoder: EntityDecoder[F, Pet] = jsonOf[F, Pet]

  implicit val statusQueryParamDecoder: QueryParamDecoder[PetStatus] =
    QueryParamDecoder[String].map(PetStatus.withName)
  object StatusMatcher extends OptionalMultiQueryParamDecoderMatcher[PetStatus]("status")
  object TagMatcher    extends OptionalMultiQueryParamDecoderMatcher[String]("tag")

  private def createPetEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "pets" =>
        val action = for {
          pet    <- req.as[Pet]
          result <- petService.create(pet).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(PetAlreadyExistsError(existing)) =>
            Conflict(s"The pet ${existing.name} of category ${existing.category} already exists")
        }
    }

  private def updatePetEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ PUT -> Root / "pets" / LongVar(petId) =>
        val action = for {
          pet <- req.as[Pet]
          updated = pet.copy(id = Some(petId))
          result <- petService.update(updated).value
        } yield result

        action.flatMap {
          case Right(saved)           => Ok(saved.asJson)
          case Left(PetNotFoundError) => NotFound("The pet was not found")
        }
    }

  private def getPetEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "pets" / LongVar(petId) =>
        petService.get(petId).value.flatMap {
          case Right(found)           => Ok(found.asJson)
          case Left(PetNotFoundError) => NotFound("The pet was not found")
        }
    }

  private def deletePetEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "pets" / LongVar(petId) =>
        for {
          _    <- petService.delete(petId)
          resp <- NoContent()
        } yield resp
    }

  private def listPetsEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "pets" :? OptionalLimitMatcher(limit) :? OptionalOffsetMatcher(offset) =>
        for {
          retrieved <- petService.list(limit.getOrElse(10), offset.getOrElse(0))
          resp      <- Ok(retrieved.asJson)
        } yield resp
    }

  private def findPetsByStatusEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "pets" / "findByStatus" :? StatusMatcher(Valid(Nil)) =>
        BadRequest("Status parameter not specified")
      case GET -> Root / "pets" / "findByStatus" :? StatusMatcher(Valid(statuses)) =>
        for {
          retrieved <- petService.findByStatus(NonEmptyList.fromListUnsafe(statuses))
          resp      <- Ok(retrieved.asJson)
        } yield resp
    }

  private def findPetsByTagEndpoint(petService: PetService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "pets" / "findByTag" :? TagMatcher(Valid(Nil)) =>
        BadRequest("Tag parameter not specified")
      case GET -> Root / "pets" / "findByTag" :? TagMatcher(Valid(tags)) =>
        for {
          retrieved <- petService.findByTag(NonEmptyList.fromListUnsafe(tags))
          resp      <- Ok(retrieved.asJson)
        } yield resp
    }

  def endpoints(petService: PetService[F]): HttpRoutes[F] =
    createPetEndpoint(petService) <+>
      updatePetEndpoint(petService) <+>
      getPetEndpoint(petService) <+>
      deletePetEndpoint(petService) <+>
      listPetsEndpoint(petService) <+>
      findPetsByStatusEndpoint(petService) <+>
      findPetsByTagEndpoint(petService)
}

object PetEndpoints {
  def endpoints[F[_]: Effect](petService: PetService[F]): HttpRoutes[F] =
    new PetEndpoints[F].endpoints(petService)
}
