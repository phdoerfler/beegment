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

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask

import Beeminder._
import AuthActor._

import scala.concurrent.Future

import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.StatusCodes._

object BeegmentService extends HttpApp with BeeminderApi with MarshallingSupport {
  implicit val system = Beegment.system
  val authActor = system.actorOf(Props[AuthActor], "auth")
  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  def refresh(goal: Goal, ot: Option[AccessToken]) = ot match {
    case Some(t) => Http() singleRequest Beeminder.requestRefresh(goal)(t)
    case None => Future.failed(new Exception("No token found"))
  }

  override def routes: Route = {
    pathSingleSlash {
      redirect(BeeminderApps.authorizeUri, PermanentRedirect)
    } ~
    pathPrefix("goal" / Slug) { goal =>
      path("refresh") {
        post {
          parameter('auth_token).as(AuthToken) { implicit token =>
            val responseFuture = Http() singleRequest Beeminder.requestRefresh(goal)
            onSuccess(responseFuture) { complete(_) }
          } ~
          parameter('username).as(Username) { username =>
            val f = for {
              ot <- (authActor ? LookupToken(username)).mapTo[Option[AccessToken]]
              r  <- refresh(goal, ot)
            } yield r
            completeOrRecoverWith(f) { failure =>
              reject(AuthorizationFailedRejection)
            }
          }
        }
      }
    } ~
    path("oauth" / "authorize") {
      get {
        parameter('access_token).as(AccessToken) { implicit token =>
          parameter('username).as(Username) { implicit username =>
            authActor ! AuthAdded(username, token)
            val help = scala.io.Source.fromResource("instructions.http").getLines map replaceUser mkString "\n"
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Beegment</h1><h2>Hi ${username.value}! You are now authorized.</h2><div>Use this as a starting point for setting up your trello webhook:</div><pre><code>$help</code></pre>"))
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

  def replaceUser(line: String)(implicit username: Username) = {
    if (line.startsWith("@u")) {
      s"@u = ${username.value}"
    } else line
  }
}