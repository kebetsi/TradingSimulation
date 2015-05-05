package ch.epfl.ts.test

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll

object TestHelpers {

  def makeTestActorSystem(name: String = "TestActorSystem") =
    ActorSystem(name, ConfigFactory.parseString(
      """
      akka.loglevel = "DEBUG"
      akka.loggers = ["akka.testkit.TestEventListener"]
      """
    ))
  
}

/**
 * Common superclass for testing actors
 * @param name Name of the actor system
 */
abstract class ActorTestSuite(val name: String)
  extends TestKit(TestHelpers.makeTestActorSystem(name))
  with WordSpecLike
  with BeforeAndAfterAll {
  
	/** After all tests have run, shut down the system */
  override def afterAll() = {
    system.shutdown()
  }
  
}