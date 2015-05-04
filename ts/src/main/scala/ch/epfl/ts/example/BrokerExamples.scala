package ch.epfl.ts

import ch.epfl.ts.component.ComponentBuilder
import akka.actor.Props
import ch.epfl.ts.traders.{SimpleTraderWithBroker}
import ch.epfl.ts.data.{MarketAskOrder, Order, Register}
import com.typesafe.config.ConfigFactory
import ch.epfl.ts.brokers.StandardBroker

/**
 * Created by sygi on 03.04.15.
 */
object BrokerExamples {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("TestingBroker", ConfigFactory.parseString("akka.loglevel = \"DEBUG\""))
    val broker = builder.createRef(Props(classOf[StandardBroker]), "Broker")

    val tId = 15L
    val trader = builder.createRef(Props(classOf[SimpleTraderWithBroker], tId), "Trader")

    trader->(broker, classOf[Register])
    trader->(broker, classOf[MarketAskOrder])

    builder.start

  }
}
