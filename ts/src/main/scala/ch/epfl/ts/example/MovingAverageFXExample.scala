package ch.epfl.ts.example

import scala.reflect.ClassTag
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import akka.actor.Props

import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.component.persist.DummyPersistor
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.engine.MarketFXSimulator
import ch.epfl.ts.traders.MovingAverageTrader
import ch.epfl.ts.component.utils.BackLoop
import ch.epfl.ts.indicators.SmaIndicator
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.data.{ Quote, OHLC }
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.indicators.{ OhlcIndicator, MaIndicator, MovingAverage, SMA }
import ch.epfl.ts.data.Currency
import ch.epfl.ts.engine.RevenueCompute
import ch.epfl.ts.component.fetch.HistDataCSVFetcher
import ch.epfl.ts.evaluation.Evaluator

object MovingAverageFXExample {
  def main(args: Array[String]): Unit = {
    val builder = new ComponentBuilder("simpleFX")
    val marketForexId = MarketNames.FOREX_ID

    val useLiveData = true
    val symbol = (Currency.EUR, Currency.CHF)
    
    // ----- Creating actors
    // Fetcher
    val fxQuoteFetcher = {
      if(useLiveData) {
    	  val fetcherFx: TrueFxFetcher = new TrueFxFetcher
        builder.createRef(Props(classOf[PullFetchComponent[Quote]], fetcherFx, implicitly[ClassTag[Quote]]), "TrueFxFetcher")
      }
      else {
        val dateFormat = new java.text.SimpleDateFormat("yyyyMM")
        val startDate = dateFormat.parse("201304");
        val endDate = dateFormat.parse("201305");
        val workingDir = "./data";
        val currencyPair = symbol._1.toString() + symbol._2.toString();
        
        builder.createRef(Props(classOf[HistDataCSVFetcher], workingDir, currencyPair, startDate, endDate, 4200.0), "HistDataFetcher")
      }
    }
    
    // Market
    val rules = new ForexMarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketFXSimulator], marketForexId, rules), MarketNames.FOREX_NAME)
    
    // Trader: cross moving average
    val traderId = 123L
    val volume = 1000.0
    val shortPeriod = 2
    val longPeriod = 6
    val periods = List(2,6)
    val tolerance = 0.0002
    val trader = builder.createRef(Props(classOf[MovingAverageTrader], traderId, symbol, shortPeriod, longPeriod, volume, tolerance, true), "MovingAverageTrader")
   
    // Indicator
    // Specify period over which we build the OHLC (from quotes)
    val period = 20000L // OHLC of 20 seconds 
    val maCross = builder.createRef(Props(classOf[SmaIndicator], periods), "maCross")
    val ohlcIndicator = builder.createRef(Props(classOf[OhlcIndicator], MarketNames.FOREX_ID, symbol, period), "OHLCIndicator")
    
    // Evaluation
    val evaluationPeriod = 2000 milliseconds
    val evaluationInitialDelay = 1000000.0
    val currency = symbol._1
    val evaluator = builder.createRef(Props(classOf[Evaluator], trader, traderId, evaluationInitialDelay, currency, evaluationPeriod), "Evaluator")

    
    // Display
    val traderNames = Map(traderId -> trader.name)

    // ----- Connecting actors
    fxQuoteFetcher -> (Seq(forexMarket, ohlcIndicator), classOf[Quote])

    trader -> (forexMarket, classOf[MarketAskOrder], classOf[MarketBidOrder])

    forexMarket -> (evaluator, classOf[Transaction])
    
    maCross -> (trader, classOf[SMA])
    ohlcIndicator -> (maCross, classOf[OHLC])
    
    builder.start
  }
}