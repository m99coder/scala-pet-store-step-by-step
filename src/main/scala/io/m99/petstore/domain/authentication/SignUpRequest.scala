package io.m99.petstore.domain.authentication

import io.m99.petstore.domain.users.User
import tsec.passwordhashers.PasswordHash

final case class SignUpRequest(userName: String,
                               firstName: String,
                               lastName: String,
                               email: String,
                               password: String) {
  def asUser[A](hashedPassword: PasswordHash[A]): User =
    User(userName, firstName, lastName, email, hashedPassword.toString)
}
