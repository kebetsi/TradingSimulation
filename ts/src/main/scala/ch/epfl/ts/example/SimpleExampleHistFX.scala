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
import ch.epfl.ts.component.fetch.HistDataCSVFetcher

object SimpleExampleHistFX {
  def main(args: Array[String]): Unit = {
    val builder = new ComponentBuilder("simpleFXHist")
    val marketForexId = MarketNames.FOREX_ID

    // ----- Creating actors
    // Fetcher
    // variables for the fetcher
    val dateFormat = new java.text.SimpleDateFormat("yyyyMM")
    val startDate = dateFormat.parse("201304");
    val endDate = dateFormat.parse("201305");
    val workingDir = "/Users/admin";
    val currencyPair = "USDCHF";

    val fetcherFx = builder.createRef(Props(classOf[HistDataCSVFetcher], workingDir, currencyPair, startDate, endDate, 4200.0),"HistFetcher")    

    // Market
    val rules = new ForexMarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketFXSimulator], marketForexId, rules), MarketNames.FOREX_NAME)

    // Trader: cross moving average
    val traderId: Long = 123L
    val symbol = (Currency.USD, Currency.CHF)
    val volume = 1000.0
    val shortPeriod = 5
    val longPeriod = 30
    val periods = List(5, 30)
    val tolerance = 0.0002
    val trader = builder.createRef(Props(classOf[SimpleFXTrader], traderId, symbol, shortPeriod, longPeriod, volume, tolerance), "simpleTrader")

    // Indicator
    // specify period over which we build the OHLC (from quotes)
    val period: Long = 60*1000 //OHLC of 1 minute  
    val maCross = builder.createRef(Props(classOf[SmaIndicator], periods), "maCross")
    val ohlcIndicator = builder.createRef(Props(classOf[OhlcIndicator], 4L, symbol, period), "ohlcIndicator")

    // Display
    val traderNames = Map(traderId -> "MovingAverageFXTrader")
    val display = builder.createRef(Props(classOf[RevenueComputeFX], traderNames), "display")

    // ----- Connecting actors
    fetcherFx -> (Seq(forexMarket, ohlcIndicator), classOf[Quote])

    trader -> (forexMarket, classOf[MarketAskOrder], classOf[MarketBidOrder])

    forexMarket -> (display, classOf[Transaction])

    maCross -> (trader, classOf[SMA])
    ohlcIndicator -> (maCross, classOf[OHLC])

    builder.start
  }
}