import sbt.Keys._
import sbt._

EclipseKeys.skipParents in ThisBuild := false

name := "TradingSimProject"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.11.2"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature")


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
        libraryDependencies ++= Dependencies.ts
    )

// Some of our tests require sequential execution
parallelExecution in Test in ts := false

parallelExecution in Test in frontend := false
