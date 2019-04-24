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

import akka.stream.ActorMaterializer

import akka.http.scaladsl.server.directives.Credentials

trait Routes { _: HttpApp with JsonSupport with BeeminderApi =>
  import com.github.nscala_time.time.Imports._
  import Beegment.system
  import system.dispatcher

  def addDatapointIfConditionIsMet(goal: Goal)(implicit t: AccessToken) = post {
    entity(as[DatapointAdded]) { dpa =>
      parameter('ifLaterThan).as(PointInTime) { startOfInterval =>
        parameter('ifBeforeThan).as(PointInTime) { endOfInterval =>
          val lateEnough = DateTime.parse(startOfInterval.value) < DateTime.now
          val earlyEnough = DateTime.now < DateTime.parse(endOfInterval.value)
          if (lateEnough && earlyEnough) {
            val f = Http() singleRequest Beeminder.addDatapoint(goal, dpa.value)
            onSuccess(f) { complete(_) }
          } else complete("Datapoint not added: condition not met")
        }
      }
    }
  }
}

object BeegmentService extends HttpApp with BeeminderApi with MarshallingSupport with JsonSupport with Routes {
  import Beegment.system

  val authActor = system.actorOf(Props[AuthActor], "auth")
  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher
  implicit val mat = ActorMaterializer() // created from `system`

  def refresh(goal: Goal, ot: Option[AccessToken]) = ot match {
    case Some(t) => Http() singleRequest Beeminder.requestRefresh(goal)(t)
    case None => Future.failed(new Exception("No token found"))
  }

  def logData(dpa: DatapointAdded) = {
    println(dpa)
  }

  def htmlForRedirect(to: Uri, body: String) = {
    s"""<html><head><meta http-equiv="refresh" content="0; URL='$to'" /></head><body>$body</body></html>"""
  }

  def authenticateViaBeeminder(creds: Credentials): Future[Option[AccessToken]] = creds match {
    case Credentials.Provided(bt) =>
      for {
        r  <- Http() singleRequest Beeminder.requestUsername(AccessToken(bt))
      } yield Some(AccessToken(bt))
    case _ => Future successful None
  }

  override def routes: Route = {
    pathSingleSlash {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlForRedirect(BeeminderApps.authorizeUri, "<h1>Beegment</h1><h2>Authorizing with Beeminder…</h2>")))
    } ~
    pathPrefix("goals" / Slug) { goal =>
      authenticateOAuth2Async("beegment", authenticateViaBeeminder) { implicit t =>
        path("refresh") {
          post {
            parameter('username).as(Username) { username =>
              val f = Http() singleRequest Beeminder.requestRefresh(goal)
              onSuccess(f) { complete(_) }
            }
          }
        } ~
        path("datapoints") {
          addDatapointIfConditionIsMet(goal)
        } ~
        path("datapoints" / "added") {
          post {
            entity(as[DatapointAdded]) { dpa =>
              parameter('username).as(Username) { username =>
                val f = for {
                  _ <- (authActor ? LookupToken(username)).mapTo[Option[AccessToken]]
                } yield logData(dpa)
                f.foreach(println)
                
                completeOrRecoverWith(f map (_ => "true")) { failure =>
                  reject(AuthorizationFailedRejection)
                }
              }
            }
          }
        }
      }
    } ~
    path("oauth" / "authorize") {
      get {
        parameter('access_token).as(AccessToken) { implicit token =>
          parameter('username).as(Username) { implicit username =>
            val nf2 = (for {
              r  <- Http() singleRequest Beeminder.requestUsername
              uf <- Unmarshal(r).to[UsernameLookup]
              u   = Username(uf.username)
            } yield {
              require(username == u, "Provided user name and looked up user name should match")
              authActor ! AuthAdded(u, token)
              val help = scala.io.Source.fromResource("instructions.http").getLines map replaceUser map replaceBase mkString "\n"
              val baseUrl = system.settings.config.getString("baseurl")
              HttpEntity(ContentTypes.`text/html(UTF-8)`, s"""<h1>Beegment</h1><h2>Hi ${username.value}! You are authorized.</h2><div><a href="$baseUrl">Again?</a></div><div>Use this as a starting point for setting up your trello webhook:</div><pre><code>$help</code></pre>""")
            })
            completeOrRecoverWith(nf2) { failure =>
              reject(AuthorizationFailedRejection)
            }
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

  def replaceBase(line: String) = {
    if (line startsWith "@dbase") {
      s"@dbase = ${system.settings.config.getString("baseurl")}"
    } else line
  }
}



