package ch.epfl.ts.example

import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.concurrent.duration.{ DurationLong }
import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.component.persist.DummyPersistor
import ch.epfl.ts.component.utils.BackLoop
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.engine.MarketFXSimulator
import ch.epfl.ts.engine.RevenueCompute
import ch.epfl.ts.engine.RevenueComputeFX
import ch.epfl.ts.indicators.MovingAverage
import ch.epfl.ts.indicators.OhlcIndicator
import ch.epfl.ts.traders.SimpleFXTrader
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.indicators.SmaIndicator
import ch.epfl.ts.data.OHLC
import ch.epfl.ts.indicators.SMA


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
    
    // Trader: cross moving average
    val traderId : Long = 123L
    val periods = List(3, 10)
    val symbol = (Currency.EUR, Currency.CHF)
    val parameters = new StrategyParameters(
      SimpleFXTrader.SYMBOL -> CurrencyPairParameter(symbol),
      SimpleFXTrader.VOLUME -> NaturalNumberParameter(10),
      SimpleFXTrader.SHORT_PERIOD -> new TimeParameter(periods(0) seconds),
      SimpleFXTrader.LONG_PERIOD -> new TimeParameter(periods(1) seconds)
    )
    val trader = SimpleFXTrader.getInstance(traderId, parameters, "SimpleFXTrader")

    // Indicator
    // specify period over which we build the OHLC (from quotes)
    // TODO: indicators should be instantiated by the trader that needs them
    val period = 20000L // OHLC of 20 seconds
    val maCross = builder.createRef(Props(classOf[SmaIndicator], periods), "maCross")
    val ohlcIndicator = builder.createRef(Props(classOf[OhlcIndicator], fetcherFx.marketId, symbol, period), "ohlcIndicator")
    
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