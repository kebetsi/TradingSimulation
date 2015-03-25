package ch.epfl.ts.example

import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.component.persist.DummyPersistor
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.engine.MarketFXSimulator
import akka.actor.Props
import ch.epfl.ts.traders.MovingAverageFXTrader
import ch.epfl.ts.component.utils.BackLoop
import ch.epfl.ts.indicators.SmaIndicator
import scala.reflect.ClassTag
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.engine.RevenueComputeFX
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder


object SimpleExampleFX {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("simpleFX")
    val marketForexId = MarketNames.FOREX_ID

    // ----- Creating actors
    // Fetcher
    val fetcherFx: TrueFxFetcher = new TrueFxFetcher
    val fxQuoteFetcher = builder.createRef(Props(classOf[PullFetchComponent[Quote]], fetcherFx, implicitly[ClassTag[Quote]]), "trueFxFetcher")
    
    // Market
    val rules = new ForexMarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketFXSimulator], marketForexId, rules), MarketNames.FOREX_NAME)
    
    // Persistor
    val dummyPersistor = new DummyPersistor()
    
    // Backloop
    val backloop = builder.createRef(Props(classOf[BackLoop], marketForexId, dummyPersistor), "backloop")
    
    // Trader: cross moving average
    val traderId : Long = 123L
    val volume = 50.0
    val shortPeriod = 50
    val longPeriod = 200
    val trader = builder.createRef(Props(classOf[MovingAverageFXTrader], traderId, shortPeriod, longPeriod, volume), "simpleTrader")
   
    // Indicators
    val smaShort = builder.createRef(Props(classOf[SmaIndicator], shortPeriod), "smaShort")
    val smaLong = builder.createRef(Props(classOf[SmaIndicator], longPeriod), "smaLong")
    
    // Display
    val traderNames = Map(traderId -> "MovingAverageFXTrader")
    val display = builder.createRef(Props(classOf[RevenueComputeFX], traderNames), "display")

    // ----- Connecting actors
    fxQuoteFetcher.addDestination(forexMarket, classOf[Quote])
    fxQuoteFetcher.addDestination(trader, classOf[Quote])

    trader.addDestination(forexMarket, classOf[MarketAskOrder])
    trader.addDestination(forexMarket, classOf[MarketBidOrder])

    forexMarket.addDestination(backloop, classOf[Transaction])
    forexMarket.addDestination(display, classOf[Transaction])

    backloop.addDestination(trader, classOf[Transaction])

    builder.start
  }
}