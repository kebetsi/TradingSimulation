package ch.epfl.ts.evaluation

import ch.epfl.ts.component.fetch.{HistDataCSVFetcher, MarketNames}
import ch.epfl.ts.component.{ComponentBuilder, ComponentRef}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data._
import ch.epfl.ts.engine.{MarketFXSimulator, ForexMarketRules}
import ch.epfl.ts.indicators.SMA

import akka.actor.Props
import ch.epfl.ts.traders.SimpleFXTrader
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * Evaluates the performance of trading strategies
 */
object EvaluationRunner {
  val builder = new ComponentBuilder("evaluation")

  def test(trader: ComponentRef, traderId: Long) = {
    val marketForexId = MarketNames.FOREX_ID

    // Fetcher
    // variables for the fetcher
    val dateFormat = new java.text.SimpleDateFormat("yyyyMM")
    val startDate = dateFormat.parse("201503");
    val endDate   = dateFormat.parse("201503");
    val workingDir = "./data";
    val currencyPair = "EURCHF";
    val fetcher = builder.createRef(Props(classOf[HistDataCSVFetcher], workingDir, currencyPair, startDate, endDate, 60.0),"HistFetcher")

    // Market
    val rules = new ForexMarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketFXSimulator], marketForexId, rules), MarketNames.FOREX_NAME)

    // Evaluator
    val period = 2000 milliseconds
    val initial = 1000000.0
    val currency = CHF
    val evaluator = builder.createRef(Props(classOf[Evaluator], trader, traderId, initial, currency, period), "evaluator")

    // ----- Connecting actors
    fetcher -> (Seq(forexMarket, evaluator), classOf[Quote])

    evaluator -> (forexMarket, classOf[MarketAskOrder], classOf[MarketBidOrder])

    builder.start
  }

  def simpleFxTrader(traderId: Long) = {
    // Trader
    val symbol = (Currency.EUR, Currency.CHF)
    val volume = 10.0
    val shortPeriod = 3
    val longPeriod = 10
    val periods=List(3,10)

    builder.createRef(Props(classOf[SimpleFXTrader], traderId,symbol, shortPeriod, longPeriod, volume), "simpleTrader")
  }

  def main(args: Array[String]): Unit = {
    test(simpleFxTrader(123L), 123L)
  }
}
