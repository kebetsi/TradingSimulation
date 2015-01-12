package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ Transaction, LimitOrder, LimitAskOrder, LimitBidOrder }
import net.liftweb.json._
import org.apache.http.client.fluent._

class BitstampTransactionPullFetcher extends PullFetch[Transaction] {
  val bitstamp = new BitstampAPI(USD, BTC)
  var count = 100
  var latest = new Transaction(MarketNames.BITSTAMP_ID, 0.0, 0.0, 0, BTC, USD, 0, 0, 0, 0)

  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = bitstamp.getTrade(count)

    val idx = trades.indexOf(latest)
    count = if (idx < 0) 100 else Math.min(10 * idx, 100)
    latest = if (trades.length == 0) latest else trades.head

    if (idx > 0)
      trades.slice(0, idx).reverse
    else
      trades.reverse
  }
}

case class BitstampCaseTransaction(date: String, tid: Int, price: String, amount: String)

class BitstampAPI(from: Currency, to: Currency) {
  implicit val formats = net.liftweb.json.DefaultFormats

  val serverBase = "https://www.bitstamp.net/api/"

  def getInfo {}

  def getTicker {}

  def getTrade(count: Int): List[Transaction] = {
    val path = serverBase + "transactions/"
    val json = Request.Get(path).execute().returnContent().asString()
    val t = parse(json).extract[List[BitstampCaseTransaction]]

    t.map(f => new Transaction(MarketNames.BITSTAMP_ID, f.price.toDouble, f.amount.toDouble, f.date.toLong * 1000, BTC, USD, 0, 0, 0, 0))
  }

  case class BitstampDepth(timestamp: String, bids: List[List[String]], asks: List[List[String]])

  def getDepth(count: Int): List[LimitOrder] = {
    var t = List[LimitOrder]()
    try {
      val path = serverBase + "/order_book/"
      val json = Request.Get(path).execute().returnContent().asString()
      println("received orders: ")
      println(json)
      val a = parse(json).extract[BitstampDepth]

      val asks = a.asks match {
        case l: List[List[String]] => l.map { e => LimitAskOrder(0, MarketNames.BITSTAMP_ID, System.currentTimeMillis, from, to, e.last.toDouble, e.head.toDouble) }
        case _                     => List[LimitOrder]()
      }
      val bids = a.bids match {
        case l: List[List[String]] => l.map { e => LimitBidOrder(0, MarketNames.BITSTAMP_ID, System.currentTimeMillis(), from, to, e.last.toDouble, e.head.toDouble) }
        case _                     => List[LimitOrder]()
      }
      t = asks ++ bids
    } catch {
      case _: Throwable => t = List[LimitOrder]()
    }
    t
  }
}