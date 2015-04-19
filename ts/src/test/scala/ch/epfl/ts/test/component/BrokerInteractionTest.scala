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
import ch.epfl.ts.engine.{ForexMarketRules, MarketFXSimulator, GetWalletFunds}
import ch.epfl.ts.component.fetch.MarketNames
import akka.util.Timeout
import ch.epfl.ts.data.Quote
import ch.epfl.ts.test.TestHelpers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

class BrokerInteractionTest
    extends TestKit(TestHelpers.makeTestActorSystem("BrokerInteractionTest"))
    with WordSpecLike {

  val marketID = 1L
  val market = system.actorOf(Props(classOf[FxMarketWrapped], marketID, new ForexMarketRules()), MarketNames.FOREX_NAME)
  val broker: ActorRef = system.actorOf(Props(classOf[SimpleBrokerWrapped], market), "Broker")
  val tId = 15L
  val trader = system.actorOf(Props(classOf[SimpleTraderWrapped], tId, broker), "Trader")

  market ! StartSignal
  broker ! StartSignal
  market ! Quote(marketID, System.currentTimeMillis(), CHF, USD, 10.2, 13.2)

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
        EventFilter.debug(message = "Broker: someone asks for not - his wallet", occurrences = 1) intercept {
          broker ! GetWalletFunds(38265L)
        }
        EventFilter.debug(message = "Broker: someone asks for not - his wallet", occurrences = 0) intercept {
          EventFilter.debug(message = "Broker: No such wallet", occurrences = 0) intercept {
            trader ! 'knowYourWallet
          }
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
        EventFilter.debug(message = "TraderWithB: Got an executed order", occurrences = 1) intercept {
          EventFilter.debug(message = "Broker: received order", occurrences = 1) intercept {
            EventFilter.debug(message = "TraderWithB: order placement succeeded", occurrences = 1) intercept {
              trader ! 'sendMarketOrder
            }
          }
        }
        EventFilter.debug(message = USD + " -> Some(97.0)", occurrences = 1) intercept {
          EventFilter.debug(start = CHF + " -> Some", occurrences = 1) intercept {
              trader ! 'knowYourWallet
          }
        }
      }
    }
  }

  "Wallet " should {
    " block the orders exceeding funds" in {
      within(1 second) {
        EventFilter.debug(message = "MarketFXSimulator : received a bidOrder", occurrences = 0) intercept {
          EventFilter.debug(message = "TraderWithB: order failed", occurrences = 1) intercept {
            trader ! 'sendTooBigOrder
          }
        }
      }
    }
  }

  "Market " should {
    " reply to the broker" in {
      within(1 second){
        EventFilter.debug(message = "Broker: Transaction executed", occurrences = 1) intercept {
          EventFilter.debug(message = "TraderWithB: Got an executed order", occurrences = 1) intercept {
            trader ! 'sendMarketOrder
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

/**
 * Analogical class for the broker.
 */
class SimpleBrokerWrapped(market: ActorRef) extends ExampleBroker {
  override def send[T: ClassTag](t: T) {
    market ! t
  }

  override def send[T: ClassTag](t: List[T]) = t.map(market ! _)
}

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

