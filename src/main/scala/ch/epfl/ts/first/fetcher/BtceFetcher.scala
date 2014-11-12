package ch.epfl.ts.first.fetcher

import ch.epfl.ts.first.TransactionPullFetch
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.first.TransactionPullFetch

import org.apache.http.client.fluent._
import net.liftweb.json._
import org.joda.time.DateTime

class BtceTransactionPullFetcher extends TransactionPullFetch {
  val btce = new BtceAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(0.0, 0.0, 0, USD, "?", "?")
  
  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = btce.getTrade(count)
    val idx = trades.indexOf(latest)
    count = if (idx < 0)  2000 else Math.min(10*idx, 2000)
    latest = if (trades.length == 0) latest else trades.head
    
    if(idx > 0)
      trades.slice(0, idx)
    else
      trades
  }
}


case class BTCeCaseTransaction(date: Long, price: Double, amount: Double, 
    tid: Int, price_currency: String, item: String, trade_type: String)

class BtceAPI(from: Currency, to: Currency) {
  implicit val formats = net.liftweb.json.DefaultFormats

  val serverBase = "https://btc-e.com/api/2/"
  val pair = pair2path

  def getInfo() {}
  def getTicker() {}

  def getTrade(count: Int): List[Transaction] = {
    var t = List[BTCeCaseTransaction]()
    try {
      var path = serverBase + pair + "/trades/" + count
      var json = Request.Get(path).execute().returnContent().asString()
      t = parse(json).extract[List[BTCeCaseTransaction]]
    } catch {
      case _ : Throwable => t = List[BTCeCaseTransaction]();
    }

    if (t.length != 0) {
      return t.map(f => new Transaction(f.price, f.amount, f.date * 1000, USD, "?", "?"))
    } else {
      return List[Transaction]()
    }
  }

  def getDepth() {}

  private def pair2path() = (from, to) match {
    case (USD, BTC) => "btc_usd"
    case (BTC, USD) => "btc_usd"
  }
}