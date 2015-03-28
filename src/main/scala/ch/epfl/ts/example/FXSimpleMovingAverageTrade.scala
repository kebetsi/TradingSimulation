package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.traders.SimpleFXTrader
import ch.epfl.ts.component.fetch.TrueFxFetcher
import scala.reflect.ClassTag
import ch.epfl.ts.component.fetch.{ BitstampOrderPullFetcher, BitstampTransactionPullFetcher, BtceOrderPullFetcher, BtceTransactionPullFetcher, MarketNames, PullFetchComponent }
import ch.epfl.ts.data.Quote
import ch.epfl.ts.engine.{ MarketRules, MarketSimulator }
import ch.epfl.ts.indicators.SmaIndicator
import ch.epfl.ts.data.{ MarketBidOrder, MarketAskOrder }
import ch.epfl.ts.indicators.MaIndicator



object FXSimpleMovingAverageTrade {
  
  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("ArbitrageSystem")
    
    // Instantiate Transaction fetchers for Forex exchange markets
    val forexQuotePullFetcher = new TrueFxFetcher
   
    //Create all the component 
    //Fetcher
    val forexQuoteFetcher = builder.createRef(Props(classOf[PullFetchComponent[Quote]], forexQuotePullFetcher, implicitly[ClassTag[Quote]]), "forexQuoteFetcher")
    

    //trader 
    val traderId : Long = 123L
    val tradingPriceDelta = 1.0
    val volume = 50.0
    val shortPeriod = 50
    val longPeriod = 200
    val trader = builder.createRef(Props(classOf[SimpleFXTrader], traderId, shortPeriod, longPeriod, volume), "simpleTrader")
   
    //Indicator
    val smaShort = builder.createRef(Props(classOf[SmaIndicator], shortPeriod), "smaShort")
    val smaLong = builder.createRef(Props(classOf[SmaIndicator], longPeriod), "smaLong")
   
    
    //Market
    val rules = new MarketRules()
    val forexMarket = builder.createRef(Props(classOf[MarketSimulator], forexQuotePullFetcher.marketId, rules), "trueFX")

    //Create the connection
    // forexQuoteFetcher.addDestination() 
    
    //allow fetcher to send data to the marketSimulator & indicators... 
    forexQuoteFetcher.addDestination(forexMarket, classOf[Quote])
    
    //allow trader to send information to the market
    trader.addDestination(forexMarket, classOf[MarketBidOrder])
    trader.addDestination(forexMarket, classOf[MarketAskOrder])
    
    //allow indicators to send information to the trader
    smaShort.addDestination(trader, classOf[MaIndicator])
    smaLong.addDestination(trader, classOf[MaIndicator])
    
    builder.start
  }
}