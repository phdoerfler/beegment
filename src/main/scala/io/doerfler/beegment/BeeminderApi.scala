package io.doerfler.beegment

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ Uri, HttpRequest }
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.http.scaladsl.Http
import scala.util.{ Failure, Success }
import Beeminder._

trait BeeminderApi {
  object Beeminder {
    def requestRefresh(goal: Goal)(implicit token: Token) = {
      v1(base / "users" / "me" / "goals" / goal.slug / "refresh_graph.json")
    }

    def base = Path / "api" / "v1"

    def v1(path: Path)(implicit token: Token): HttpRequest = {
      HttpRequest(uri = Uri.from(scheme = "https", host = "www.beeminder.com", path = path.toString) withQuery Query(fromToken(token)))
    }

    def fromToken(token: Token) = token match {
      case AuthToken(s)   => "auth_token" -> s
      case AccessToken(s) => "access_token" -> s
    }
  }

  object BeeminderApps {
    def authorizeUri = {
      Uri.from(scheme = "https", host = "www.beeminder.com", path = "/apps/authorize") withQuery Query(
        "client_id" -> "9xieoto9lhsk0fjuf7upzoaz7",
        "redirect_uri" -> "https://doerfler.io:8042/oauth/authorize",
        "response_type" -> "token")
    }
  }
}