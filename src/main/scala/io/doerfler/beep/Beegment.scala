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


object BeegmentService extends HttpApp with BeeminderSupport {
  def Slug = Segment map Goal

  override def routes: Route = withSystem { implicit system =>
    pathPrefix("goal" / Slug) { goal =>
      path("refresh") {
        post {
          parameters(('auth_token.as[String])).as(AuthToken) { implicit token =>
            val responseFuture = Http() singleRequest Beeminder.requestRefresh(goal)
            onSuccess(responseFuture) { complete(_) }
          }
        }
      }
    } ~
    path("oauth") {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<div>You can now use Beegment!</div>"))
    }
  }
  
  def withSystem(f: (ActorSystem) => Route): Route = extractActorSystem { f }
}

object Beegment extends App {
  BeegmentService.startServer("localhost", 8040)
}