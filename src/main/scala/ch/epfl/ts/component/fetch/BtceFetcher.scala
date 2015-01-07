package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{Order, Transaction}
import net.liftweb.json._
import org.apache.http.client.fluent._

class BtceTransactionPullFetcher extends PullFetch[Transaction] {
  val btce = new BtceAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(0.0, 0.0, 0, BTC, USD, 0, 0, 0, 0)
  
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

class BtceOrderPullFetcher extends PullFetch[Order] {
  val btce = new BtceAPI(USD, BTC)
  var count = 2000
  override def interval(): Int = 12000
  override def fetch(): List[Order] = btce.getDepth(count)
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
      val path = serverBase + pair + "/trades/" + count
      val json = Request.Get(path).execute().returnContent().asString()
      t = parse(json).extract[List[BTCeCaseTransaction]]
    } catch {
      case _ : Throwable => t = List[BTCeCaseTransaction]();
    }

    if (t.length != 0) {
      t.map(f => new Transaction(f.price, f.amount, f.date * 1000, BTC, USD, 0, 0, 0, 0))
    } else {
      List[Transaction]()
    }
  }

  def getDepth(count: Int): List[Order] = {
    var t = List[Order]()
    try {
      val path = serverBase + pair + "/depth/" + count
      val json = Request.Get(path).execute().returnContent().asString()
      val a = parse(json).extract[Map[String, List[List[Double]]]]
      
      val asks = a.get("asks") match {
//        case Some(l) => l.map(e => Order(0, e.head, e.last, 0, from, OrderType.ASK))
        case _ => List[Order]()
      }
      val bids = a.get("bids") match {
//        case Some(l) => l.map(e => Order(0, e.head, e.last, 0, from, OrderType.BID))
        case _ => List[Order]()
      }
      t = asks ++ bids
    } catch {
      case _ : Throwable => t = List[Order]()
    }
    t
  }

  private def pair2path() = (from, to) match {
    case (USD, BTC) => "btc_usd"
    case (BTC, USD) => "btc_usd"
  }
}