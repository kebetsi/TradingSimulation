package ch.epfl.bigdata.btc.crawler.coins.markets

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Transaction

import org.apache.http.client.fluent._
import net.liftweb.json._
import org.joda.time.DateTime


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

  def getDepth() {

  }

  private def pair2path() = (from, to) match {
    case (USD, BTC) => "btc_usd"
    case (BTC, USD) => "btc_usd"
  }
}