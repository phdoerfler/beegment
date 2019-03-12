package io.doerfler.echobee

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._


trait TokenAuthentication {
  implicit val `String => AuthToken for query parameters`: QueryParamDecoder[AuthToken] =
    QueryParamDecoder[String].map(AuthToken)
  object AuthTokenParamMatcher extends QueryParamDecoderMatcher[AuthToken]("auth_token")

  def authorized(uri: Uri)(implicit authToken: AuthToken) = uri withQueryParam("auth_token", authToken.value)
}

case class AuthToken(value: String)