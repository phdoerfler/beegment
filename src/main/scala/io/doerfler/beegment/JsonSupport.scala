package io.doerfler.beegment

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val usernameLookupFormat: RootJsonFormat[UsernameLookup] = jsonFormat1(UsernameLookup)
  case class UsernameLookup(username: String)

  implicit val dpAddedFormat: RootJsonFormat[DatapointAdded] = jsonFormat4(DatapointAdded)
  case class DatapointAdded(goal: String, date: String, value: String, comment: String)
}