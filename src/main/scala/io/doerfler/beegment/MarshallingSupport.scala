package io.doerfler.beegment

import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.server.PathMatchers
import Beeminder._

trait MarshallingSupport extends PathMatchers {
  def Slug = Segment map Goal
  implicit def `String <-> AccessToken`: FromEntityUnmarshaller[AccessToken] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller map AccessToken
}