package io.doerfler.beep

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ Uri, HttpRequest }
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.http.scaladsl.Http
import scala.util.{ Failure, Success }


object BeepService extends HttpApp with BeeminderSupport {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  def Slug = Segment map Goal

  override def routes: Route =
    pathPrefix("goal" / Slug) { goal =>
      path("refresh") {
        post {
          parameters(('auth_token.as[String])).as(AuthToken) { implicit token =>
            val responseFuture = Http() singleRequest Beeminder.requestRefresh(goal)
            onSuccess(responseFuture) { complete(_) }
          }
        }
      }
    }
}

trait BeeminderSupport {
  object Beeminder {
    def requestRefresh(goal: Goal)(implicit token: AuthToken) = {
      v1(base / "users" / "me" / "goals" / goal.slug / "refresh_graph.json")
    }

    def v1(path: Path)(implicit token: AuthToken): HttpRequest = {
      HttpRequest(uri = Uri.from(scheme = "https", host = "www.beeminder.com", path = path.toString) withQuery Query("auth_token" -> token.value))
    }

    def base = Path / "api" / "v1"
  }

  case class Goal(slug: String)
  case class AuthToken(value: String)
}

object Beep extends App {
  BeepService.startServer("localhost", 8040)
}