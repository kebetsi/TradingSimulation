package ch.epfl.ts.test.traders

import scala.language.postfixOps
import scala.concurrent.duration.DurationInt
import org.scalatest.WordSpecLike
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.EventFilter
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.indicators.SMA
import ch.epfl.ts.traders.MovingAverageTrader
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import ch.epfl.ts.test.TestHelpers
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.WalletParameter
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.RealNumberParameter
import ch.epfl.ts.engine.Wallet
import ch.epfl.ts.data.BooleanParameter
import akka.actor.ActorRef
import scala.reflect.ClassTag
import ch.epfl.ts.brokers.StandardBroker
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.engine.MarketFXSimulator
import akka.util.Timeout
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.data._
import scala.math.floor

@RunWith(classOf[JUnitRunner])
class MovingAverageTraderTest
  extends TestKit(TestHelpers.makeTestActorSystem("MovingAverageTraderTestSystem"))
  with WordSpecLike {

  val traderId: Long = 123L
  val symbol = (Currency.USD, Currency.CHF)
  val initialFunds: Wallet.Type = Map(symbol._2 -> 5000.0)
  val periods = Seq(5, 30)
  val tolerance = 0.0002

  val parameters = new StrategyParameters(
    MovingAverageTrader.INITIAL_FUNDS -> WalletParameter(initialFunds),
    MovingAverageTrader.SYMBOL -> CurrencyPairParameter(symbol),
    MovingAverageTrader.SHORT_PERIOD -> new TimeParameter(periods(0)),
    MovingAverageTrader.LONG_PERIOD -> new TimeParameter(periods(1)),
    MovingAverageTrader.TOLERANCE -> RealNumberParameter(tolerance),
    MovingAverageTrader.WITH_SHORT -> BooleanParameter(false))

  val marketID = 1L
  val market = system.actorOf(Props(classOf[FxMarketWrapped], marketID, new ForexMarketRules()), MarketNames.FOREX_NAME)
  val broker: ActorRef = system.actorOf(Props(classOf[SimpleBrokerWrapped], market), "Broker")
  val trader = system.actorOf(Props(classOf[MovingAverageTraderWrapped], traderId, parameters, broker), "Trader")

  market ! StartSignal
  broker ! StartSignal
  trader ! StartSignal

  val (bidPrice, askPrice) = (0.90, 0.95)
  val testQuote = Quote(marketID, System.currentTimeMillis(), symbol._1, symbol._2, bidPrice, askPrice)
  market ! testQuote
  //TODO Quote are received from the market
  broker ! testQuote
  trader ! testQuote

  /**
   * @warning The following tests are dependent and should be executed in the specified order.
   */
  val initWallet = initialFunds;
  var cash = initialFunds(Currency.CHF)
  var volume = floor(cash / askPrice)

  "A trader " should {
    "buy (20,3)" in {
      within(1 second) {
        EventFilter.debug(message = "buying " + volume, occurrences = 1) intercept {
          trader ! SMA(Map(5 -> 20.0, 30 -> 3.0))
        }
      }
      cash -= volume * askPrice
    }

    "sell(3,20)" in {
      within(1 second) {
        EventFilter.debug(message = "selling " + volume, occurrences = 1) intercept {
          trader ! SMA(Map(5 -> 3.0, 30 -> 20.0))
        }
      }
      cash += volume * bidPrice
      volume = floor(cash / askPrice)
    }

    "not buy(10.001,10)" in {
      within(1 second) {
        EventFilter.debug(message = "buying " + volume, occurrences = 0) intercept {
          trader ! SMA(Map(5 -> 10.001, 30 -> 10.0))
        }
      }
    }

    // For small numbers > is eq to >=  (10*(1+0.0002) = 10.00199999)
    "buy(10.002,10)" in {
      within(1 second) {
        EventFilter.debug(message = "buying " + volume, occurrences = 1) intercept {
          trader ! SMA(Map(5 -> 10.002, 30 -> 10))
        }
      }
      cash -= volume * askPrice
    }

    "not buy(10.003,10) (already hold a position)" in {
      within(1 second) {
        EventFilter.debug(message = "buying " + volume, occurrences = 0) intercept {
          trader ! SMA(Map(5 -> 10.003, 30 -> 10))
        }
      }
    }

    "sell(9.9999,10)" in {
      within(1 second) {
        EventFilter.debug(message = "selling " + volume, occurrences = 1) intercept {
          trader ! SMA(Map(5 -> 9.9999, 30 -> 10))
        }
      }
      cash += volume * bidPrice
      volume = floor(cash / askPrice)
    }

    "not sell(9.9999,10) (no holding)" in {
      within(1 second) {
        EventFilter.debug(message = "selling " + volume, occurrences = 0) intercept {
          trader ! SMA(Map(5 -> 9.9999, 30 -> 10))
        }
      }
    }
  }
}
/**
 * A bit dirty hack to allow ComponentRef-like communication between components, while having them in Test ActorSystem
 * @param uid traderID
 * @param StrategyParameters parameters
 * @param broker ActorRef
 */
class MovingAverageTraderWrapped(uid: Long, parameters: StrategyParameters, broker: ActorRef)
  extends MovingAverageTrader(uid, parameters) {
  override def send[T: ClassTag](t: T) {
    broker ! t
  }
  override def send[T: ClassTag](t: List[T]) = t.map(broker ! _)
}

/**
 * Analogical class for the broker.
 */
class SimpleBrokerWrapped(market: ActorRef) extends StandardBroker {
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

