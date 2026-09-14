name := "hicenter-backend-scala"
organization := "com.hicenter"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "hicenter-backend-scala",
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      guice,
      "org.postgresql" % "postgresql" % "42.7.3",
      "org.mindrot" % "jbcrypt" % "0.4",
      "com.github.jwt-scala" %% "jwt-play-json" % "10.0.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    ),
    scalacOptions ++= Seq(
      "-feature"
    )
  )
