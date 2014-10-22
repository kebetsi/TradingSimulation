import sbt._
import Process._
import Keys._

name := "TradingSimProject"

version := "0.1"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.3.6",
	"com.typesafe.slick" %% "slick" % "2.1.0"
)
