package ch.epfl.ts

import ch.epfl.ts.component.ComponentBuilder
import akka.actor.Props
import ch.epfl.ts.traders.{SimpleTraderWithBroker}
import ch.epfl.ts.brokers.ExampleBroker
import ch.epfl.ts.data.Register

/**
 * Created by sygi on 03.04.15.
 */
object BrokerExamples {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("TestingBroker")
    val broker = builder.createRef(Props(classOf[ExampleBroker]), "Broker")

    val tId = 15L
    val trader = builder.createRef(Props(classOf[SimpleTraderWithBroker], tId), "Trader")

    trader.addDestination(broker, classOf[Register])

    builder.start

  }
}
