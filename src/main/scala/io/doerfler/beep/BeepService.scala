package io.doerfler.echobee

import cats.effect._
import io.circe.Json
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext.Implicits.global

object Beeminder {
  def base = Uri.uri("https://www.beeminder.com/api/v1/")
  def refresh(goal: String) = base / "users" / "me" / "goals" / goal / "refresh_graph.json"
}

class BeepService[F[_]: Effect] extends Http4sDsl[F] with TokenAuthentication {
  val service: HttpService[F] = HttpService[F] {
    case POST -> Root / "refresh" / goal :? AuthTokenParamMatcher(authToken) =>
      implicit val at = authToken
      val httpClient = Http1Client[IO]().unsafeRunSync
      val refreshGoalRequest = httpClient.expect[String](authorized(Beeminder refresh goal))
      val result = refreshGoalRequest.unsafeRunSync()
      httpClient.shutdownNow()
      Ok(result)
  }

}
