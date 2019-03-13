lazy val akkaHttpVersion = "10.1.7"
lazy val akkaVersion    = "2.5.21"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "doerfler.io",
      scalaVersion    := "2.12.8"
    )),
    name := "Beep",
    version := "1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test
    )
  )

enablePlugins(PackPlugin)
packMain := Map("beep" -> "io.doerfler.beep.Beep")
packJvmOpts := Map("beep" -> Seq("-Xmx16m"))