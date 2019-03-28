package io.doerfler

package object beegment {
  object Beeminder {
    case class Goal(slug: String)
    case class Username(value: String)

    sealed trait Token {
      def value: String
    }
    case class AuthToken(value: String) extends Token
    case class AccessToken(value: String) extends Token
  }

  type :=>[A, B] = PartialFunction[A, B]
}