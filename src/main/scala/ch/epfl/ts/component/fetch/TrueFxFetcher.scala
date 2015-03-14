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
  // TODO: what should this marketId be?
  val marketId = -1
  
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
      /** Prices are separated in "big figure" and "points" */
      val bid = values(1) + (values(2) * 1e-5)
      val ask = values(3) + (values(4) * 1e-5)
      
    	Quote(
         marketId, timestamp,
         currencies(0), currencies(1),
         bid, ask
      )
    }
  }
  
  def interval(): Int = 5000
}