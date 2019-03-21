package io.doerfler.beegment

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling._

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ Uri, HttpRequest }
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.http.scaladsl.Http
import scala.util.{ Failure, Success, Try }
import akka.Done

import akka.actor._

import Beeminder._
import AuthActor._

object BeegmentService extends HttpApp with BeeminderApi with MarshallingSupport {
  implicit val system = Beegment.system
  val authActor = system.actorOf(Props[AuthActor], "auth")

  override def routes: Route = {
    pathPrefix("goal" / Slug) { goal =>
      path("refresh") {
        post {
          parameter('auth_token).as(AuthToken) { implicit token =>
            val responseFuture = Http() singleRequest Beeminder.requestRefresh(goal)
            onSuccess(responseFuture) { complete(_) }
          }
        }
      }
    } ~
    path("oauth" / "authorize") {
      get {
        parameter('access_token).as(AccessToken) { implicit token =>
          parameter('username).as(Username) { username =>
            authActor ! AuthAdded(username, token)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Authorization Granted</h1><h2>Hi ${username.value}!</h2>"))
          }
        }
      }
    } ~
    path("oauth" / "deauthorize") {
      post {
        entity(as[AccessToken]) { token =>
          authActor ! AuthRemoved(token)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Authorization Revoked</h1>"))
        }
      }
    }
  }
}