package ch.epfl.ts

import com.typesafe.config.ConfigFactory

import akka.actor.Props
import ch.epfl.ts.brokers.ExampleBroker
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.Register
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.traders.SimpleTraderWithBroker

/**
 * Example main with a broker and wallet-aware trader
 */
object BrokerExamples {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("TestingBroker", ConfigFactory.parseString("akka.loglevel = \"DEBUG\""))
    val broker = builder.createRef(Props(classOf[ExampleBroker]), "Broker")

    val tId = 15L
    val trader = SimpleTraderWithBroker.getInstance(tId, new StrategyParameters(), "BrokerAwareTrader")

    trader->(broker, classOf[Register])
    trader->(broker, classOf[MarketAskOrder])

    builder.start

  }
}
