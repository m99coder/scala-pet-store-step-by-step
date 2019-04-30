package io.m99.petstore.infrastructure.repository.doobie

import cats.Monad
import cats.data.{NonEmptyList, OptionT}
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.{fragments => Fragments}
import doobie.util.Meta
import doobie.util.fragment.Fragment
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import io.m99.petstore.domain.pets.{Pet, PetRepositoryAlgebra, PetStatus}

private object PetSQL {

  // we require `StatusMeta` to handle our ADT `PetStatus`
  implicit val StatusMeta: Meta[PetStatus] =
    Meta[String].imap(PetStatus.withName)(_.entryName)

  // we require `SetStringMeta` to marshal our sets of strings
  implicit val SetStringMeta: Meta[Set[String]] =
    Meta[String].imap(_.split(',').toSet)(_.mkString(","))

  def insert(pet: Pet): Update0 =
    sql"""INSERT INTO pets(name, category, bio, status, tags, photo_urls)
          VALUES (${pet.name}, ${pet.category}, ${pet.bio}, ${pet.status}, ${pet.tags}, ${pet.photoUrls})
      """.update

  def update(pet: Pet, id: Long): Update0 =
    sql"""UPDATE pets
          SET name = ${pet.name}, bio = ${pet.bio}, status = ${pet.status}, tags = ${pet.tags}, photo_urls = ${pet.photoUrls}
          WHERE id = $id
      """.update

  def select(id: Long): Query0[Pet] =
    sql"""SELECT name, category, bio, status, tags, photo_urls, id
          FROM pets
          WHERE id = $id
      """.query

  def delete(id: Long): Update0 =
    sql"""DELETE FROM pets WHERE id = $id""".update

  def selectByNameAndCategory(name: String, category: String): Query0[Pet] =
    sql"""SELECT name, category, bio, status, tags, photo_urls, id
          FROM pets
          WHERE name = $name AND category = $category
       """.query[Pet]

  def selectAll: Query0[Pet] =
    sql"""SELECT name, category, bio, status, tags, photo_urls, id
          FROM pets
          ORDER BY name
       """.query

  def selectByStatus(statuses: NonEmptyList[PetStatus]): Query0[Pet] =
    (sql"""SELECT name, category, bio, status, tags, photo_urls, id
           FROM pets
           WHERE """ ++ Fragments.in(fr"status", statuses)).query

  def selectTagLikeString(tags: NonEmptyList[String]): Query0[Pet] = {
    val tagLikeString: String = tags.toList.mkString("tags LIKE '%", "%' OR tags LIKE '%", "%'")
    (sql"""SELECT name, category, bio, status, tags, photo_urls, id
           FROM pets
           WHERE """ ++ Fragment.const(tagLikeString)).query[Pet]
  }
}

class DoobiePetRepositoryInterpreter[F[_]: Monad](val transactor: Transactor[F])
    extends PetRepositoryAlgebra[F] {
  import PetSQL._

  def create(pet: Pet): F[Pet] =
    insert(pet)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => pet.copy(id = id.some))
      .transact(transactor)

  def update(pet: Pet): F[Option[Pet]] =
    OptionT
      .fromOption[ConnectionIO](pet.id)
      .semiflatMap(id => PetSQL.update(pet, id).run.as(pet))
      .value
      .transact(transactor)

  def get(id: Long): F[Option[Pet]] = select(id).option.transact(transactor)

  def delete(id: Long): F[Option[Pet]] =
    OptionT(get(id)).semiflatMap(pet => PetSQL.delete(id).run.transact(transactor).as(pet)).value

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    selectByNameAndCategory(name, category).to[List].transact(transactor).map(_.toSet)

  def list(limit: Int, offset: Int): F[List[Pet]] =
    SQLPagination.paginate(limit, offset)(selectAll).to[List].transact(transactor)

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    selectByStatus(statuses).to[List].transact(transactor)

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    selectTagLikeString(tags).to[List].transact(transactor)

}

object DoobiePetRepositoryInterpreter {
  def apply[F[_]: Monad](transactor: Transactor[F]): DoobiePetRepositoryInterpreter[F] =
    new DoobiePetRepositoryInterpreter[F](transactor)
}
