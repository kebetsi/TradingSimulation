import sbt._
import Process._
import Keys._

name := "Engine"

version := "0.1"

scalaVersion := "2.11.2"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"
