import sbt.Keys._
import sbt._

EclipseKeys.skipParents in ThisBuild := false

name := "TradingSimProject"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.11.2"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature")

resolvers in ThisBuild += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

lazy val frontend = (project in file("frontend"))
    .enablePlugins(PlayScala)
    .settings(
        name := "frontend",
        libraryDependencies ++= (Dependencies.frontend  ++ Seq(filters, cache)),
        pipelineStages := Seq(rjs, digest, gzip)
    ).dependsOn(ts).aggregate(ts)

lazy val ts = (project in file("ts"))
    .settings(
        name := "ts",
        libraryDependencies ++= Dependencies.ts,
        // Add res directory to runtime classpath
        unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/main/resources") }
    )

// Some of our tests require sequential execution
parallelExecution in Test in ts := false

parallelExecution in Test in frontend := false
