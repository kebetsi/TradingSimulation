package ch.epfl.ts.test

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object TestHelpers {

  def makeTestActorSystem(name: String = "TestActorSystem") =
    ActorSystem(name, ConfigFactory.parseString(
      """
      akka.loglevel = "DEBUG"
      akka.loggers = ["akka.testkit.TestEventListener"]
      """
    ))
  
}