package io.m99.petstore.infrastructure.repository.doobie

import doobie.implicits._
import doobie.util.Read
import doobie.util.query.Query0

trait SQLPagination {
  def limit[A: Read](l: Int)(q: Query0[A]): Query0[A] =
    (q.toFragment ++ fr"LIMIT $l").query
  def paginate[A: Read](limit: Int, offset: Int)(q: Query0[A]): Query0[A] =
    (q.toFragment ++ fr"LIMIT $limit OFFSET $offset").query
}

object SQLPagination extends SQLPagination
