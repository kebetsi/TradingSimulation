import sbt._
import Process._
import Keys._

name := "TradingSimProject"

version := "0.1"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
	"com.github.nscala-time" %% "nscala-time" % "1.4.0" withSources() withJavadoc(),
	"com.typesafe.akka" %% "akka-actor" % "2.3.6" withSources() withJavadoc(),
	"com.typesafe.slick" %% "slick" % "2.1.0" withSources() withJavadoc(),
	"net.liftweb" %% "lift-json" % "2.6-RC1" withSources() withJavadoc(),
	"org.apache.httpcomponents" % "fluent-hc" % "4.3.6" withSources() withJavadoc(),
	"org.slf4j" % "slf4j-nop" % "1.6.4" withSources() withJavadoc(),
	"org.xerial" % "sqlite-jdbc" % "3.8.7" withSources() withJavadoc(),
	"org.twitter4j" % "twitter4j-stream" % "3.0.3"
)
