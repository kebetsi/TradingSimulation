package ch.epfl.ts.test.traders

import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.WordSpecLike
import akka.testkit.TestKit
import ch.epfl.ts.data.CoefficientParameter
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.test.TestHelpers
import ch.epfl.ts.traders.MadTrader

@RunWith(classOf[JUnitRunner])
class MadTraderTest
  extends TestKit(TestHelpers.makeTestActorSystem("MadTraderTest"))
  with FunSuiteLike {
  
  val traderId = 42L
  val emptyParameters = new StrategyParameters()
  val legalParameters = new StrategyParameters(
      MadTrader.CURRENCY_PAIR -> CurrencyPairParameter(Currency.EUR, Currency.CHF),
      MadTrader.INTERVAL -> new TimeParameter(1 seconds),
      MadTrader.ORDER_VOLUME -> NaturalNumberParameter(100)
  )
  
  
  // TODO: refactor generic strategy testing from `StrategyParameter` test suite?
  // TODO: actual tests for the `MadTrader` strategy (check volume, interval, etc)
}