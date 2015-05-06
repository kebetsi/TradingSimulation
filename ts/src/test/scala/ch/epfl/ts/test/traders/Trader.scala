package ch.epfl.ts.test.traders

import scala.util.Try

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import ch.epfl.ts.traders.Arbitrageur
import ch.epfl.ts.traders.TransactionVwapTrader
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.traders.MadTrader
import ch.epfl.ts.traders.SobiTrader
import ch.epfl.ts.traders.MovingAverageTrader
import ch.epfl.ts.traders.TraderCompanion
import ch.epfl.ts.traders.SimpleTraderWithBroker
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.test.ActorTestSuite

@RunWith(classOf[JUnitRunner])
class TraderTestSuite
  extends ActorTestSuite("ConcreteStrategyTest") {

  /** Simple tests for strategy's parameterization */
  new ConcreteStrategyTestSuite(MadTrader)
  new ConcreteStrategyTestSuite(TransactionVwapTrader)
  new ConcreteStrategyTestSuite(MovingAverageTrader)
  new ConcreteStrategyTestSuite(SimpleTraderWithBroker)
  new ConcreteStrategyTestSuite(Arbitrageur)
  new ConcreteStrategyTestSuite(SobiTrader)

  /**
   * Generic function to test concrete trading strategy implementation's correct
   * behavior when instantiated with correct & incorrect parameters
   */
  class ConcreteStrategyTestSuite(val strategyCompanion: TraderCompanion)
                                  (implicit builder: ComponentBuilder) {

    var traderId = 42L
    def make(p: StrategyParameters) = {
      // Need to give actors a unique name
      val suffix = System.currentTimeMillis() + (Math.random() * 100000L).toLong
      val name = "TraderBeingTested-" + suffix.toString
      strategyCompanion.getInstance(traderId, p, name)
    }
    val emptyParameters = new StrategyParameters()
    val required = strategyCompanion.requiredParameters
    val optional = strategyCompanion.optionalParameters

    // TODO: test optional parameters

    /**
     * Strategies not having any required parameter
     */
    if(required.isEmpty) {
      "A " + strategyCompanion.toString() should {
        "should allow instantiation with no parameters" in {
          val attempt = Try(make(emptyParameters))
          assert(attempt.isSuccess, attempt.failed)
        }
      }
    }
    /**
     * Strategies with required parameters
     */
    else {
      "A " + strategyCompanion.toString() should {
        "not allow instantiation with no parameters" in {
          val attempt = Try(make(emptyParameters))
          assert(attempt.isFailure)
        }

        "should allow instantiation with parameters' default values" in {
          val parameters = for {
            pair <- strategyCompanion.requiredParameters.toSeq
            key = pair._1
            parameter = pair._2.getInstance(pair._2.defaultValue)
          } yield (key, parameter)

          val sp = new StrategyParameters(parameters: _*)
          val attempt = Try(make(sp))
          assert(attempt.isSuccess, attempt.failed)
        }
      }
    }
  }
}
