package io.doerfler.beegment

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ Uri, HttpRequest }
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.http.scaladsl.Http
import scala.util.{ Failure, Success }


object BeegmentService extends HttpApp with BeeminderSupport {
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
    } ~
    path("oauth") {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<div>You can now use Beep!</div>"))
    }
}

object Beegment extends App {
  BeegmentService.startServer("localhost", 8040)
}