package ch.epfl.ts.test.traders

import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.WordSpecLike
import akka.testkit.TestKit
import akka.testkit.EventFilter
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.data.CoefficientParameter
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.test.TestHelpers
import ch.epfl.ts.traders.MadTrader
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.data.WalletParameter
import ch.epfl.ts.engine.Wallet

@RunWith(classOf[JUnitRunner])
class MadTraderTest
  extends TestKit(TestHelpers.makeTestActorSystem("MadTraderTest"))
  with WordSpecLike {
  
  val traderId = 42L
  val currencies = (Currency.EUR, Currency.CHF)
  val initialFunds: Wallet.Type = Map(currencies._2 -> 1000.0)
  val initialDelay =  100 milliseconds
  val interval = 50 milliseconds
  val volume = 100
  val volumeVariation = 0.1
  
  /** Give a little margin of error in our timing assumptions */
  val gracePeriod = (10 milliseconds)
  
  val parameters = new StrategyParameters(
      MadTrader.INITIAL_FUNDS -> WalletParameter(initialFunds),
      MadTrader.CURRENCY_PAIR -> CurrencyPairParameter(currencies),
      MadTrader.INITIAL_DELAY -> new TimeParameter(initialDelay),
      MadTrader.INTERVAL -> new TimeParameter(interval),
      MadTrader.ORDER_VOLUME -> NaturalNumberParameter(volume),
      MadTrader.ORDER_VOLUME_VARIATION -> CoefficientParameter(volumeVariation)
  )
  
  // TODO: refactor generic strategy testing from `StrategyParameter` test suite?
  // TODO: refactor common Actor testing characteristics into a common superclass (word specs, testkit, builder, ...)
  implicit val builder = new ComponentBuilder("MadTraderTest")
  val trader = MadTrader.getInstance(traderId, parameters, "MadTrader")
  
  
  "A MadTrader" should {
    "send its first order within the given initial delay" in {
      within(initialDelay + gracePeriod) {
        // TODO
        assert(true)
      }
    }
    
    "send orders regularly based on the given interval" in {
      within(initialDelay + interval + 2 * gracePeriod) {
        // TODO
        assert(true)
      }
    }
    
    "respect respect the given volume" in {
      // TODO
    }
  }
  
}