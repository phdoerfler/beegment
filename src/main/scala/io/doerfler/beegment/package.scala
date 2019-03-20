package io.doerfler

package object beegment {
  object Beeminder {
    case class Goal(slug: String)
    case class AuthToken(value: String)
    case class AccessToken(value: String)
    case class Username(value: String)
  }
}