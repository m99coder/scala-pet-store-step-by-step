package io.m99.petstore.infrastructure.endpoint

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object Pagination {
  object OptionalLimitMatcher  extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OptionalOffsetMatcher extends OptionalQueryParamDecoderMatcher[Int]("offset")
}
