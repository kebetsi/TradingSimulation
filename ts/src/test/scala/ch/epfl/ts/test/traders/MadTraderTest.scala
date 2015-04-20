package ch.epfl.ts.test.traders

import scala.language.postfixOps
import scala.util.Try
import org.junit.runner.RunWith
import org.scalatest.FunSuiteLike
import org.scalatest.WordSpecLike
import org.scalatest.junit.JUnitRunner
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestKit
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.test.TestHelpers
import ch.epfl.ts.traders.MadTrader
import ch.epfl.ts.data.TimeParameter
import scala.concurrent.duration.DurationLong
import ch.epfl.ts.component.ComponentBuilder

@RunWith(classOf[JUnitRunner])
class MadTraderTest
  extends TestKit(TestHelpers.makeTestActorSystem("MadTraderTest"))
  with FunSuiteLike {

  // TODO: refactor all Akka-oriented test helpers into a trait
  implicit val builder = new ComponentBuilder("MadTraderTest")
  
  val traderId = 42L
  val emptyParameters = new StrategyParameters()
  val legalParameters = new StrategyParameters(
      MadTrader.CURRENCY_PAIR -> CurrencyPairParameter(Currency.EUR, Currency.CHF),
      MadTrader.INTERVAL -> new TimeParameter(1 seconds),
      MadTrader.ORDER_VOLUME -> NaturalNumberParameter(100)
  )
    
  test("Should not allow instantiation without any parameters") {
    val attempt = Try( MadTrader.getInstance(traderId, emptyParameters) )
    assert(attempt.isFailure)
  }
  
  test("Should allow instantiation with only required parameters") {
    val trader = MadTrader.getInstance(traderId, legalParameters, "MyMadTrader")
    assert(trader.name === "MyMadTrader")
	  //val attempt = Try( MadTrader.getInstance(traderId, legalParameters) )
		//assert(attempt.isSuccess)
  }
}