lazy val akkaHttpVersion = "10.2.10"
lazy val akkaVersion    = "2.6.20"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "doerfler.io",
      scalaVersion    := "2.12.17",
      scalacOptions ++= Seq("language:_")
    )),
    name := "Beegment",
    version := "1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence"     % akkaVersion,

      "org.iq80.leveldb"            % "leveldb"          % "0.12",
      "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",

      "com.github.nscala-time" %% "nscala-time" % "2.32.0",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test
    )
  )

enablePlugins(PackPlugin)
packMain := Map("beegment" -> "io.doerfler.beegment.Beegment")
packJvmOpts := Map("beegment" -> Seq("-Xmx16m"))