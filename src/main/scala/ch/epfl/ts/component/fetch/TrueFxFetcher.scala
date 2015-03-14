package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.Quote
import org.apache.http.client.fluent.Request
import ch.epfl.ts.data.OHLC

/**
 * Fetcher for the TrueFX HTTP API, which provides live Forex quotes for free
 * @see TrueFX dev documentation: http://www.truefx.com/dev/data/TrueFX_MarketDataWebAPI_DeveloperGuide.pdf
 */
class TrueFxFetcher extends PullFetch[Quote]{
  val serverBase = "http://webrates.truefx.com/rates/connect.html" + "?f=csv"
  val marketId = MarketNames.FOREX_ID
  
  def fetch(): List[Quote] = {
    val csv = Request.Get(serverBase).execute().returnContent().asString()
    
    for {
      line <- csv.split('\n').toList
      if line.length() > 1 // Eliminate the last empty line
    } yield {
      
  	  val fields = line.split(',')
		  val currencies = fields(0).split('/').map(s => Currency.fromString(s.toLowerCase))
		  val timestamp = fields(1).toLong
		  val values = fields.drop(1).map(s => s.toDouble)
      
      /**
       * Prices are separated in "big figure" and "points".
       * We can simply concatenate them to obtain the full price.
       */
      val bid = (fields(2) + fields(3)).toDouble
      val ask = (fields(4) + fields(5)).toDouble
      
    	Quote(
         marketId, timestamp,
         currencies(0), currencies(1),
         bid, ask
      )
    }
  }
  
  def interval(): Int = 5000
}