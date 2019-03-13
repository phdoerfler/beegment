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

object Beep extends App {
  BeepService.startServer("localhost", 8040)
}