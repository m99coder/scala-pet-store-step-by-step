package io.m99.petstore.infrastructure.repository.doobie

import cats.Monad
import cats.data.OptionT
import cats.syntax.functor._
import cats.syntax.option._
import doobie.util.update.Update0
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import io.m99.petstore.domain.users.{User, UserRepositoryAlgebra}

private object UserSQL {
  def insert(user: User): Update0 =
    sql"""INSERT INTO users (user_name, first_name, last_name, email, password)
          VALUES (${user.userName}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.password})
      """.update

  def update(user: User, id: Long): Update0 =
    sql"""UPDATE users
          SET first_name = ${user.firstName}, last_name = ${user.lastName}, email = ${user.email}, password = ${user.password}
          WHERE id = $id
      """.update

  def select(userId: Long): Query0[User] =
    sql"""SELECT user_name, first_name, last_name, email, password, id
          FROM users
          WHERE id = $userId
      """.query

  def selectByUserName(userName: String): Query0[User] =
    sql"""SELECT user_name, first_name, last_name, email, password, id
          FROM users
          WHERE user_name = $userName
      """.query

  def delete(userId: Long): Update0 =
    sql"""DELETE FROM users WHERE id = $userId""".update

  val selectAll: Query0[User] =
    sql"""SELECT user_name, first_name, last_name, email, password, id
          FROM users
      """.query
}

class DoobieUserRepositoryInterpreter[F[_]: Monad](val transactor: Transactor[F])
    extends UserRepositoryAlgebra[F] {
  import UserSQL._

  def create(user: User): F[User] =
    insert(user)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => user.copy(id = id.some))
      .transact(transactor)

  def update(user: User): F[Option[User]] =
    OptionT
      .fromOption[F](user.id)
      .semiflatMap { id =>
        UserSQL.update(user, id).run.transact(transactor).as(user)
      }
      .value

  def get(userId: Long): F[Option[User]] = select(userId).option.transact(transactor)

  def findByUserName(userName: String): F[Option[User]] =
    selectByUserName(userName).option.transact(transactor)

  def delete(userId: Long): F[Option[User]] =
    OptionT(get(userId))
      .semiflatMap(user => UserSQL.delete(userId).run.transact(transactor).as(user))
      .value

  def deleteByUserName(userName: String): F[Option[User]] =
    OptionT(findByUserName(userName)).mapFilter(_.id).flatMapF(delete).value

  def list(limit: Int, offset: Int): F[List[User]] =
    SQLPagination.paginate(limit, offset)(selectAll).to[List].transact(transactor)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Monad](transactor: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(transactor)
}
