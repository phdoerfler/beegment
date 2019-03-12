package io.doerfler.echobee

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult

class RefreshSpec extends org.specs2.mutable.Specification with TokenAuthentication {

  "RefreshService" >> {
    "return 200" >> {
      uriReturns200()
    }
    "return true" >> {
      uriReturnsTrue()
    }
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    refreshMr.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsTrue(): MatchResult[String] =
    refreshMr.as[String].unsafeRunSync() must beEqualTo("true")
  
  private[this] val refreshMr: Response[IO] = {
    val getHW = Request[IO](Method.POST, authorized(Uri.uri("/refresh/mr")))
    new BeepService[IO].service.orNotFound(getHW).unsafeRunSync()
  }
}
