package ch.epfl.ts.example

import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.Quote
import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.engine.MarketSimulator
import akka.actor.Props
import scala.reflect.ClassTag
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.traders.SimpleFXTrader
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.Quote
import ch.epfl.ts.engine.RevenueCompute
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.engine.MarketFXSimulator
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.engine.RevenueComputeFX
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.component.utils.{BackLoop, Printer}
import ch.epfl.ts.component.persist.DummyPersistor


object SimpleExampleFX {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("simpleFX")
    val marketForexId = MarketNames.FOREX_ID

    //Creating actors:
    //fetcher
    val fetcherFx: TrueFxFetcher = new TrueFxFetcher
    val fxQuoteFetcher = builder.createRef(Props(classOf[PullFetchComponent[Quote]], fetcherFx, implicitly[ClassTag[Quote]]), "trueFxFetcher")
    //market
    val rules = new ForexMarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketFXSimulator], marketForexId, rules), MarketNames.FOREX_NAME)
    //persistor
    val dummyPersistor = new DummyPersistor()
    //backloop
    val backloop = builder.createRef(Props(classOf[BackLoop], marketForexId, dummyPersistor), "backloop")
    //trader
    val simpleFXTrader = builder.createRef(Props(classOf[SimpleFXTrader], 132L), "simpleFXTrader")
    val traderNames = Map(132L->"simpleFXTrader")
    //display
    val display = builder.createRef(Props(classOf[RevenueComputeFX],traderNames), "display")

    //connections:
    fxQuoteFetcher.addDestination(forexMarket, classOf[Quote])
    fxQuoteFetcher.addDestination(simpleFXTrader, classOf[Quote])

    simpleFXTrader.addDestination(forexMarket, classOf[MarketAskOrder])
    simpleFXTrader.addDestination(forexMarket, classOf[MarketBidOrder])

    forexMarket.addDestination(backloop, classOf[Transaction])
    forexMarket.addDestination(display, classOf[Transaction])

    backloop.addDestination(simpleFXTrader, classOf[Transaction])

    builder.start
  }
}