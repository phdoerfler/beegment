package io.doerfler.echobee

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object BeepServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream[IO]
}

object ServerStream {

  def beepService[F[_]: Effect] = new BeepService[F].service

  def stream[F[_]: Effect](implicit ec: ExecutionContext) =
    BlazeBuilder[F]
      .bindHttp(8027, "0.0.0.0")
      .mountService(beepService, "/")
      .serve
}
