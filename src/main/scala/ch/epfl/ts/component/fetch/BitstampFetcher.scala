package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Transaction
import net.liftweb.json._
import org.apache.http.client.fluent._


class BitstampTransactionPullFetcher extends PullFetch[Transaction] {
  val bitstamp = new BitstampAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(0.0, 0.0, 0, BTC, USD, 0, 0, 0, 0)

  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = bitstamp.getTrade(count)

    val idx = trades.indexOf(latest)
    count = if (idx < 0) 2000 else Math.min(10 * idx, 2000)
    latest = if (trades.length == 0) latest else trades.head

    if (idx > 0)
      trades.slice(0, idx)
    else
      trades
  }
}

case class BitstampCaseTransaction(date: String, tid: Int, price: String, amount: String)


class BitstampAPI(from: Currency, to: Currency) {
  implicit val formats = net.liftweb.json.DefaultFormats

  val serverBase = "https://www.bitstamp.net/api/transactions/"

  def getInfo {}

  def getTicker {}

  def getTrade(count: Int): List[Transaction] = {
    val path = serverBase
    val json = Request.Get(path).execute().returnContent().asString()
    val t = parse(json).extract[List[BitstampCaseTransaction]]

    t.map(f => new Transaction(f.price.toDouble, f.amount.toDouble, f.date.toLong * 1000, BTC, USD, 0, 0, 0, 0))
  }

  def getDepth {}
}