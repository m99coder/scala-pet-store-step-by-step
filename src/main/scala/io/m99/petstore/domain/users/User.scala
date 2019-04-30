package io.m99.petstore.domain.users

case class User(userName: String,
                firstName: String,
                lastName: String,
                email: String,
                password: String,
                id: Option[Long] = None)
