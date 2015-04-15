import sbt._

object Dependencies {

  lazy val frontend = common ++ webjars
  lazy val ts = common ++ tradingsimulation

  val common = Seq(
    "junit" % "junit" % "4.8.1" % "test",
    "org.scalatest" % "scalatest_2.11" % "2.2.2" % "test",
    "com.typesafe.akka" %% "akka-actor" % "2.3.6" withSources() withJavadoc(),
    "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
    "com.typesafe.slick" %% "slick" % "2.1.0" withSources() withJavadoc()
  )

  val webjars = Seq(
    "org.webjars" % "requirejs" % "2.1.11-1",
    "org.webjars" % "underscorejs" % "1.6.0-3",
    "org.webjars" % "jquery" % "1.11.1",
    "org.webjars" % "highstock" % "2.0.4",
    "org.webjars" % "bootstrap" % "3.2.0" exclude ("org.webjars", "jquery"),
    "org.webjars" % "bootswatch-yeti" % "3.2.0" exclude ("org.webjars", "jquery"),
    "org.webjars" % "angularjs" % "1.2.16-2" exclude ("org.webjars", "jquery")
  )

  val tradingsimulation = Seq(
    "net.liftweb" %% "lift-json" % "2.6-RC1" withSources() withJavadoc(),
    "org.apache.httpcomponents" % "fluent-hc" % "4.3.6" withSources() withJavadoc(),
    "org.twitter4j" % "twitter4j-stream" % "3.0.3" withSources() withJavadoc(),
    "org.xerial" % "sqlite-jdbc" % "3.8.7" withSources() withJavadoc()
  )


}

