package ch.epfl.ts.test

import scala.language.postfixOps
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll
import ch.epfl.ts.engine.MarketFXSimulator
import ch.epfl.ts.brokers.StandardBroker
import ch.epfl.ts.engine.ForexMarketRules
import scala.reflect.ClassTag
import akka.actor.ActorRef
import akka.util.Timeout
import ch.epfl.ts.component.ComponentBuilder

object TestHelpers {

  def makeTestActorSystem(name: String = "TestActorSystem") =
    ActorSystem(name, ConfigFactory.parseString(
      """
      akka.loglevel = "DEBUG"
      akka.loggers = ["akka.testkit.TestEventListener"]
      """
    ).withFallback(ConfigFactory.load()))
  
}

/**
 * Common superclass for testing actors
 * @param name Name of the actor system
 */
abstract class ActorTestSuite(val name: String)
  extends TestKit(TestHelpers.makeTestActorSystem(name))
  with WordSpecLike
  with BeforeAndAfterAll {
  
  implicit val builder = new ComponentBuilder(system)
  
	/** After all tests have run, shut down the system */
  override def afterAll() = {
    // TODO: use system.terminate (for some reason, doesn't compile in SBT)
    system.shutdown()
    system.awaitTermination()
  }
  
}

/**
 * A bit dirty hack to allow ComponentRef-like communication between components, while having them in Test ActorSystem
 */
class SimpleBrokerWrapped(market: ActorRef) extends StandardBroker {
  override def send[T: ClassTag](t: T) {
    market ! t
  }

  override def send[T: ClassTag](t: List[T]) = t.map(market ! _)
}

/**
 * A bit dirty hack to allow ComponentRef-like communication between components, while having them in Test ActorSystem
 */
class FxMarketWrapped(uid: Long, rules: ForexMarketRules) extends MarketFXSimulator(uid, rules) {
  import context.dispatcher
  override def send[T: ClassTag](t: T) {
    val broker = context.actorSelection("../Broker")
    implicit val timeout = new Timeout(100 milliseconds)
    for (res <- broker.resolveOne()) {
      res ! t
    }
  }
}
