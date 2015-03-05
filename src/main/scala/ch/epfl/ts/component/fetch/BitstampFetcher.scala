package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitOrder, LimitAskOrder, LimitBidOrder, Order, Transaction}
import net.liftweb.json.parse
import org.apache.http.client.fluent._

/**
 * Implementation of the Transaction Fetch API for Bitstamp
 */
class BitstampTransactionPullFetcher extends PullFetch[Transaction] {
  val bitstamp = new BitstampAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(MarketNames.BITSTAMP_ID, 0.0, 0.0, 0, BTC, USD, 0, 0, 0, 0)

  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = bitstamp.getTrade(count)

    val idx = trades.indexOf(latest)
    count = if (idx < 0) 2000 else Math.min(10 * idx, 100)
    latest = if (trades.length == 0) latest else trades.head

    if (idx > 0)
      trades.slice(0, idx).reverse
    else
      trades.reverse
  }
}

/**
 * Implementation of the Orders Fetch API for Bitstamp
 */
class BitstampOrderPullFetcher extends PullFetch[Order] {
  val bitstampApi = new BitstampAPI(USD, BTC)
  var count = 2000
  // Contains the OrderId and The fetch timestamp
  var oldOrderBook = Map[Order, (Long, Long)]()
  var oid = 10000000000L

  override def interval(): Int = 12000

  override def fetch(): List[Order] = {
    val fetchTime = System.currentTimeMillis()

    // Fetch the new Orders
    val curOrderBook = bitstampApi.getDepth(count)

    // find which are new by computing the difference: newOrders = currentOrders - oldOrders
    val newOrders = curOrderBook diff oldOrderBook.keySet.toList
    val delOrders = oldOrderBook.keySet.toList diff curOrderBook

    // Indexes deleted orders and removes them from the map
    val indexedDelOrders = delOrders map { k =>
      val oidts: (Long, Long) = oldOrderBook.get(k).get
      oldOrderBook -= k
      k match {
        case LimitBidOrder(o, u, ft, wac, wic, v, p) => DelOrder(oidts._1, oidts._1, oidts._2, wac, wic, v, p)
        case LimitAskOrder(o, u, ft, wac, wic, v, p) => DelOrder(oidts._1, oidts._1, oidts._2, wac, wic, v, p)
      }
    }

    // Indexes new orders and add them to the map
    val indexedNewOrders = newOrders map { k =>
      oid += 1
      val order = k match {
        case LimitAskOrder(o, u, ft, wac, wic, v, p) => LimitAskOrder(oid, u, fetchTime, wac, wic, v, p)
        case LimitBidOrder(o, u, ft, wac, wic, v, p) => LimitBidOrder(oid, u, fetchTime, wac, wic, v, p)
      }
      oldOrderBook += (k ->(oid, fetchTime))
      order
    }

    indexedNewOrders ++ indexedDelOrders
  }
}

private[this] case class BitstampTransaction(date: String, tid: Int, price: String, amount: String)

private[this] case class BitstampDepth(timestamp: String, bids: List[List[String]], asks: List[List[String]])

class BitstampAPI(from: Currency, to: Currency) {
  implicit val formats = net.liftweb.json.DefaultFormats

  val serverBase = "https://www.bitstamp.net/api/"

  /**
   * Fetches count transactions from Bitstamp's HTTP trade API
   * @param count number of Transactions to fetch
   * @return the fetched transactions
   */
  def getTrade(count: Int): List[Transaction] = {
    val path = serverBase + "transactions/"
    val json = Request.Get(path).execute().returnContent().asString()
    val t = parse(json).extract[List[BitstampTransaction]]

    t.map(f => Transaction(MarketNames.BITSTAMP_ID, f.price.toDouble, f.amount.toDouble, f.date.toLong * 1000, BTC, USD, 0, 0, 0, 0))
  }

  /**
   * Fetches count orders from Bitstamp's orderbook using the HTTP order API
   * @param count number of Order to fetch
   * @return the fetched orders
   */
  def getDepth(count: Int): List[LimitOrder] = {
    var t = List[LimitOrder]()
    try {
      val path = serverBase + "order_book/"
      val json = Request.Get(path).execute().returnContent().asString()
      val o = parse(json).extract[BitstampDepth]

      val asks = o.asks.map { e => LimitAskOrder(0, 0, 0L, USD, BTC, e.last.toDouble, e.head.toDouble)}
      val bids = o.bids.map { e => LimitBidOrder(0, 0, 0L, USD, BTC, e.last.toDouble, e.head.toDouble)}

      t = asks ++ bids
    } catch {
      case e: Throwable =>
        t = List[LimitOrder]()
    }
    t
  }
}