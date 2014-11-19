package ch.epfl.ts.first.fetcher

import ch.epfl.ts.first.TransactionPullFetch
import ch.epfl.ts.first.OrderPullFetch
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.data.Order
import ch.epfl.ts.data.OrderType
import ch.epfl.ts.data.Currency._

import org.apache.http.client.fluent._
import org.joda.time.DateTime
import net.liftweb.json._


class BitstampTransactionPullFetcher extends TransactionPullFetch {
  val bitstamp = new BitstampAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(0.0, 0.0, 0, USD, "?", "?")
  
  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = bitstamp.getTrade(count)
    
    val idx = trades.indexOf(latest)
    count = if (idx < 0)  2000 else Math.min(10*idx, 2000)
    latest = if (trades.length == 0) latest else trades.head
    
    if(idx > 0)
      trades.slice(0, idx)
    else
      trades
  }
}

case class BitstampCaseTransaction(date: String, tid: Int, price: String, amount: String)


class BitstampAPI(from: Currency, to: Currency) {
  implicit val formats = net.liftweb.json.DefaultFormats
  
  val serverBase = "https://www.bitstamp.net/api/transactions/"
 
  def getInfo() {
    
  }
  
  def getTicker() {
    
  }
  
  def getTrade(count: Int) : List[Transaction] = {
    var path = serverBase
    var json = Request.Get(path).execute().returnContent().asString()
    
    var t = parse(json).extract[List[BitstampCaseTransaction]]

    return t.map(f => new Transaction(f.price.toDouble, f.amount.toDouble, f.date.toLong * 1000, USD, "?", "?"))
  }
  
  def getDepth() {
    
  }
}