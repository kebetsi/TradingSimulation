package ch.epfl.ts.test.component

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{EventFilter, TestKit}
import ch.epfl.ts.component.{StartSignal, ComponentBuilder}
import ch.epfl.ts.brokers.ExampleBroker
import ch.epfl.ts.traders.SimpleTraderWithBroker
import org.scalatest.WordSpecLike
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.language.postfixOps
import scala.reflect.ClassTag

/**
 * Created by sygi on 07.04.15.
 */
class BrokerInteractionTest extends TestKit(ActorSystem("BrokerInteractionTest", ConfigFactory.parseString(
  """
  akka.loglevel = "DEBUG"
  akka.loggers = ["akka.testkit.TestEventListener"]
  """
)))
    with WordSpecLike {

  val broker: ActorRef = system.actorOf(Props(classOf[ExampleBroker]), "Broker")
  val tId = 15L
  val trader = system.actorOf(Props(classOf[SimpleTraderWrapped], tId, broker), "Trader")

  broker ! StartSignal

  "A trader " should {
    " register in a broker on startSignal" in {
      within(4000 millis) {
        EventFilter.debug(message = "TraderWithB: Broker confirmed", occurrences = 1) intercept {
          EventFilter.debug(message = "TraderWithB received startSignal", occurrences = 1) intercept {
            trader ! StartSignal
          }
        }
      }
    }
  }
}

/**
 * A bit dirty hack to allow ComponentRef-like communication between components, while having them in Test ActorSystem
 * @param uid traderID
 * @param broker ActorRef
 */
class SimpleTraderWrapped(uid: Long, broker: ActorRef) extends SimpleTraderWithBroker(uid) {
  override def send[T: ClassTag](t: T) {
    broker ! t
  }

  override def send[T: ClassTag](t: List[T]) = t.map(broker ! _)
}