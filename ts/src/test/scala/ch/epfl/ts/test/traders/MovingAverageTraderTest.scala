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
import ch.epfl.ts.data.Currency
import ch.epfl.ts.indicators.SMA
import ch.epfl.ts.traders.MovingAverageTrader
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class MovingAverageTraderTest extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(
  """
  akka.loglevel = "DEBUG"
  akka.loggers = ["akka.testkit.TestEventListener"]
  """))) with WordSpecLike {

  val traderId: Long = 123L
  val symbol = (Currency.USD, Currency.CHF)
  val volume = 1000.0
  val shortPeriod = 5
  val longPeriod = 30
  val periods = List(5, 30)
  val tolerance = 0.0002
 
  //TODO Useless here (for the moment)
  val initialFund = 5000.0;
  val initialCurrency=Currency.CHF

  val trader = TestActorRef(Props(classOf[MovingAverageTrader], traderId, symbol,initialFund,initialCurrency, shortPeriod, longPeriod, volume, tolerance,false))
  trader ! StartSignal

  /**
   * @warning The following tests are dependent and should be executed in the specified order.
   */
  "A trader " should {
    "buy (20,3)" in {
      within(1 second) {
        EventFilter.debug(message = "buying " + volume, occurrences = 1) intercept {
          trader ! SMA(Map(5 -> 20.0, 30 -> 3.0))
        }
      }
    }

    "sell(3,20)" in {
      within(1 second) {
        EventFilter.debug(message = "selling " + volume, occurrences = 1) intercept {
          trader ! SMA(Map(5 -> 3.0, 30 -> 20.0))
        }
      }
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