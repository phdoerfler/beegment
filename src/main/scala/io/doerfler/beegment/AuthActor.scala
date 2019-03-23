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
import akka.persistence._

import Beeminder._


class AuthActor extends PersistentActor with BeeminderApi {
  import AuthActor._

  override def persistenceId = "users-1"

  var state: UsersState = UsersState()

  val receiveRecover: Receive = {
    case ae: AuthEvent                          => updateState(ae)
    case SnapshotOffer(_, snapshot: UsersState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case ae: AuthEvent ⇒
      persist(ae) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        self ! "print"
      }
    case LookupToken(u) =>
      sender ! state.users.get(u)

    case "print" ⇒
      println(state)
  }

  def updateState(event: AuthEvent): Unit = state = state.updated(event)
}

object AuthActor {
  sealed trait AuthEvent
  case class AuthAdded(user: Username, token: AccessToken) extends AuthEvent
  case class AuthRemoved(token: AccessToken) extends AuthEvent

  case class LookupToken(user: Username)

  case class UsersState(users: Map[Username, AccessToken] = Map.empty) {
    def updated(event: AuthEvent): UsersState = event match {
      case aa: AuthAdded => copy(users = users + (AuthAdded unapply aa get))
      case AuthRemoved(t) => copy(users = users.filterNot { case (_, token) => token == t })
    }
    def size = users.size
  }
}

