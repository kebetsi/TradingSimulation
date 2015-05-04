package ch.epfl.ts

import com.typesafe.config.ConfigFactory
import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.traders.SimpleTraderWithBroker
import ch.epfl.ts.data.{MarketAskOrder, Order, Register, StrategyParameters}
import com.typesafe.config.ConfigFactory
import ch.epfl.ts.brokers.StandardBroker
import ch.epfl.ts.data.WalletParameter
import ch.epfl.ts.data.Currency

/**
 * Example main with a broker and wallet-aware trader
 */
object BrokerExamples {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("TestingBroker", ConfigFactory.parseString("akka.loglevel = \"DEBUG\""))
    val broker = builder.createRef(Props(classOf[StandardBroker]), "Broker")

    val tId = 15L
    val parameters = new StrategyParameters(
      SimpleTraderWithBroker.INITIAL_FUNDS -> WalletParameter(Map(Currency.CHF -> 1000.0))
    )
    val trader = SimpleTraderWithBroker.getInstance(tId, parameters, "BrokerAwareTrader")

    trader->(broker, classOf[Register])
    trader->(broker, classOf[MarketAskOrder])

    builder.start

  }
}
