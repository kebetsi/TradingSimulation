package ch.epfl.ts.test.component

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{EventFilter, TestKit}
import ch.epfl.ts.component.{StartSignal}
import ch.epfl.ts.brokers.ExampleBroker
import ch.epfl.ts.traders.SimpleTraderWithBroker
import org.scalatest.WordSpecLike
import scala.concurrent.duration._
import ch.epfl.ts.data.Currency._
import com.typesafe.config.ConfigFactory
import scala.language.postfixOps
import scala.reflect.ClassTag
import ch.epfl.ts.engine.GetWalletFunds

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
      within(1 second) {
        EventFilter.debug(message = "TraderWithB: Broker confirmed", occurrences = 1) intercept {
          EventFilter.debug(message = "TraderWithB received startSignal", occurrences = 1) intercept {
            trader ! StartSignal
          }
        }
      }
    }
  }
  //this executes sequentially - don't change the order
  "A broker " should {
    " create a wallet for the trader" in {
      within(1 second) {
        EventFilter.debug(message = "Broker: No such wallet", occurrences = 1) intercept {
          broker ! GetWalletFunds(38265L)
        }
        EventFilter.debug(message = "Broker: No such wallet", occurrences = 0) intercept {
          broker ! GetWalletFunds(15L) //this causes dead letters, as broker replies
        }
      }
    }
  }

  "A trader " can {
    " add funds to his wallet" in {
      //TODO(sygi): can he, actually?
      within(1 second) {
        EventFilter.debug(message = "TraderWithB: trying to add 100 bucks", occurrences = 1) intercept {
          EventFilter.debug(message = "Broker: got a request to fund a wallet", occurrences = 1) intercept {
            EventFilter.debug(message = "TraderWithB: Got a wallet confirmation", occurrences = 1) intercept {
              trader ! 'addFunds
            }
          }
        }
      }
    }
    " check the state of his wallet" in {
      within(1 second) {
        EventFilter.debug(message = USD + " -> Some(100.0)", occurrences = 1) intercept {
          EventFilter.debug(message = "TraderWithB: money I have: ", occurrences = 1) intercept {
            EventFilter.debug(message = "Broker: got a get show wallet request", occurrences = 1) intercept {
              trader ! 'knowYourWallet
            }
          }
        }
      }
    }
    " place the order" in {
      within(1 second) {
        EventFilter.debug(message = "Broker: received order", occurrences = 1) intercept {
          EventFilter.debug(message = "TraderWithB: order succeeded", occurrences = 1) intercept {
            trader ! 'sendMarketOrder
          }
        }
      }
    }
  }

  "Wallet " should {
    " block the orders exceeding funds" in {
      within(1 second) {
        EventFilter.debug(message = "TraderWithB: order failed", occurrences = 1) intercept {
          trader ! 'sendTooBigOrder
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