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
import ch.epfl.ts.component.fetch.HistDataCSVFetcher
import ch.epfl.ts.evaluation.Evaluator
import ch.epfl.ts.indicators.EmaIndicator
import ch.epfl.ts.brokers.StandardBroker
import ch.epfl.ts.data.Register
import ch.epfl.ts.engine.FundWallet
import com.typesafe.config.ConfigFactory
import ch.epfl.ts.indicators.EMA
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.engine.ExecutedAskOrder
import ch.epfl.ts.engine.ExecutedBidOrder
import ch.epfl.ts.data.Order
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.engine.Wallet
import ch.epfl.ts.data.WalletParameter
import ch.epfl.ts.data.RealNumberParameter

object MovingAverageFXExample {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("simpleFX", ConfigFactory.parseString("akka.loglevel = \"DEBUG\""))
    val marketForexId = MarketNames.FOREX_ID

    val useLiveData = false
    val symbol = (Currency.USD, Currency.CHF)

    // ----- Creating actors
    // Fetcher
    val fxQuoteFetcher = {
      if (useLiveData) {
        val fetcherFx: TrueFxFetcher = new TrueFxFetcher
        builder.createRef(Props(classOf[PullFetchComponent[Quote]], fetcherFx, implicitly[ClassTag[Quote]]), "TrueFxFetcher")
      } else {
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
    val periods = List(2, 6)
    val initialFunds: Wallet.Type = Map(Currency.CHF -> 5000.0)
    val parameters = new StrategyParameters(
      MovingAverageTrader.INITIAL_FUNDS -> WalletParameter(initialFunds),
      MovingAverageTrader.SYMBOL -> CurrencyPairParameter(symbol),
      MovingAverageTrader.VOLUME -> NaturalNumberParameter(1000),
      MovingAverageTrader.SHORT_PERIOD -> new TimeParameter(periods(0) seconds),
      MovingAverageTrader.LONG_PERIOD -> new TimeParameter(periods(1) seconds),
      MovingAverageTrader.TOLERANCE -> RealNumberParameter(0.0002)
    )

    val trader = MovingAverageTrader.getInstance(traderId, parameters, "MovingAverageTrader")


    // Indicator
    // Specify period over which we build the OHLC (from quotes)
    val period = 3600000L // OHLC of 1 hour
    val maCross = builder.createRef(Props(classOf[EmaIndicator], periods), "maCross")
    val ohlcIndicator = builder.createRef(Props(classOf[OhlcIndicator], MarketNames.FOREX_ID, symbol, period), "OHLCIndicator")

    // Evaluation
    val evaluationPeriod = 2000 milliseconds
    val evaluationInitialDelay = 1000000.0
    val currency = symbol._1
//    val evaluator = builder.createRef(Props(classOf[Evaluator], trader, traderId, evaluationInitialDelay, currency, evaluationPeriod), "Evaluator")

    // Broker
    val broker = builder.createRef(Props(classOf[StandardBroker]), "Broker")

    // Display
    val traderNames = Map(traderId -> trader.name)
    // Add printer if needed to debug / display

    // ----- Connecting actors
    fxQuoteFetcher -> (Seq(forexMarket, ohlcIndicator,broker), classOf[Quote])

    trader -> (broker,classOf[Register],classOf[FundWallet],classOf[GetWalletFunds],classOf[MarketAskOrder], classOf[MarketBidOrder])
    broker->(forexMarket,classOf[MarketAskOrder], classOf[MarketBidOrder])
    forexMarket -> (broker,classOf[ExecutedBidOrder],classOf[ExecutedAskOrder])

    maCross -> (trader, classOf[EMA])
    ohlcIndicator -> (maCross, classOf[OHLC])

    // ----- Start
    builder.start
  }
}
