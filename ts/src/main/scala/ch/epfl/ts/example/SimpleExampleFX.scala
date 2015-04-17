package ch.epfl.ts.example

import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.component.persist.DummyPersistor
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.engine.MarketFXSimulator
import akka.actor.Props
import ch.epfl.ts.traders.SimpleFXTrader
import ch.epfl.ts.component.utils.BackLoop
import ch.epfl.ts.indicators.SmaIndicator
import scala.reflect.ClassTag
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.engine.RevenueComputeFX
import ch.epfl.ts.data.{ Quote, OHLC }
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.indicators.{ OhlcIndicator, MaIndicator, MovingAverage, SMA }
import ch.epfl.ts.data.Currency
import ch.epfl.ts.engine.RevenueCompute


object SimpleExampleFX {
  def main(args: Array[String]): Unit = {
    val builder = new ComponentBuilder("simpleFX")
    val marketForexId = MarketNames.FOREX_ID

    // ----- Creating actors
    // Fetcher
    val fetcherFx: TrueFxFetcher = new TrueFxFetcher
    val fxQuoteFetcher = builder.createRef(Props(classOf[PullFetchComponent[Quote]], fetcherFx, implicitly[ClassTag[Quote]]), "trueFxFetcher")
    
    // Market
    val rules = new ForexMarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketFXSimulator], marketForexId, rules), MarketNames.FOREX_NAME)
    
    // Trader: cross moving average
    val traderId : Long = 123L
    val symbol = (Currency.EUR,Currency.USD)
    val volume = 10.0
    val shortPeriod = 3
    val longPeriod = 10
    val periods=List(3,10)
    val trader = builder.createRef(Props(classOf[SimpleFXTrader], traderId,symbol, shortPeriod, longPeriod, volume), "simpleTrader")
   
    // Indicator
    // specify period over which we build the OHLC (from quotes)
    val period : Long = 20000 //OHLC of 20 seconds 
    val maCross = builder.createRef(Props(classOf[SmaIndicator], periods), "maCross")
    val ohlcIndicator = builder.createRef(Props(classOf[OhlcIndicator], fetcherFx.marketId,symbol, period), "ohlcIndicator")
    
    // Display
    val traderNames = Map(traderId -> "MovingAverageFXTrader")
    val display = builder.createRef(Props(classOf[RevenueComputeFX], traderNames), "display")

    // ----- Connecting actors
    fxQuoteFetcher -> (Seq(forexMarket, ohlcIndicator), classOf[Quote])

    trader -> (forexMarket, classOf[MarketAskOrder], classOf[MarketBidOrder])

    forexMarket -> (display, classOf[Transaction])
    
    maCross -> (trader, classOf[SMA])
    ohlcIndicator -> (maCross, classOf[OHLC])
    
    builder.start
  }
}