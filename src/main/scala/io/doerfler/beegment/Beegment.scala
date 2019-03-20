package io.doerfler.beegment

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ Uri, HttpRequest }
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.http.scaladsl.Http
import scala.util.{ Failure, Success, Try }
import akka.Done

import akka.actor._

import Beeminder._
import AuthActor._

object BeegmentService extends HttpApp with BeeminderApi {
  import Beegment.system

  def Slug = Segment map Goal

  val authActor = system.actorOf(Props[AuthActor], "auth")

  authActor ! "print"

  override def routes: Route = withSystem { implicit system =>
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
    path("oauth" / "landing") {
      get {
        parameter('access_token).as(AccessToken) { implicit token =>
          parameter('username).as(Username) { username =>
            authActor ! AuthAdded(username, token)
            authActor ! "print"
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Beegment authorized</h1><h2>Hi $username!</h2>"))
          }
        }
      }
    }
  }

  def withSystem(f: (ActorSystem) => Route): Route = extractActorSystem { f }
}

object Beegment extends App {
  val system = ActorSystem("universe")
  BeegmentService.startServer("localhost", 8040, system)
  system.terminate()
}