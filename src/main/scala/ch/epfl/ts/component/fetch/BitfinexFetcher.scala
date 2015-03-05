package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitOrder, LimitAskOrder, LimitBidOrder, Order, Transaction}
import net.liftweb.json.parse
import org.apache.http.client.fluent._

/**
 * Implementation of the Transaction Fetch API for Bitfinex
 */
class BitfinexTransactionPullFetcher extends PullFetch[Transaction] {
  val btce = new BitfinexAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(0, 0.0, 0.0, 0, BTC, USD, 0, 0, 0, 0)

  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = btce.getTrade(count)
    val idx = trades.indexOf(latest)
    count = if (idx < 0) 2000 else Math.min(10 * idx, 2000)
    latest = if (trades.length == 0) latest else trades.head

    if (idx > 0)
      trades.slice(0, idx)
    else
      trades
  }
}

/**
 * Implementation of the Orders Fetch API for Bitfinex
 */
class BitfinexOrderPullFetcher extends PullFetch[Order] {
  val bitstampApi = new BitfinexAPI(USD, BTC)
  var count = 2000
  // Contains the OrderId and The fetch timestamp
  var oldOrderBook = Map[Order, (Long, Long)]()
  var oid = 15000000000L

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

private[this] case class BitfinexCaseTransaction(timestamp: Long, tid: Int, price: String, amount: String, exchange: String)

private[this] case class BitfinexOrder(price: String, amount: String, timestamp: String)

private[this] case class BitfinexDepth(bids: List[BitfinexOrder], asks: List[BitfinexOrder])

class BitfinexAPI(from: Currency, to: Currency) {
  implicit val formats = net.liftweb.json.DefaultFormats
  val serverBase = "https://api.bitfinex.com/v1/"
  val pair = pair2path

  /**
   * Fetches count transactions from Bitfinex's HTTP trade API
   * @param count number of Transactions to fetch
   * @return the fetched transactions
   */
  def getTrade(count: Int): List[Transaction] = {
    val path = serverBase + "/trades/" + pair
    val json = Request.Get(path).execute().returnContent().asString()
    val o = parse(json).extract[List[BitfinexCaseTransaction]]

    if (o.length != 0) {
      o.map(f => new Transaction(0, f.price.toDouble, f.amount.toDouble, f.timestamp, BTC, USD, 0, 0, 0, 0))
    } else {
      List[Transaction]()
    }
  }

  /**
   * Fetches count orders from Bitfinex's orderbook using the HTTP order API
   * @param count number of Order to fetch
   * @return the fetched orders
   */
  def getDepth(count: Int): List[LimitOrder] = {
    var t = List[LimitOrder]()
    try {
      val path = serverBase + "/book/" + pair + "?limit_bids=" + count / 2 + "&limit_asks=2000" + count / 2
      val json = Request.Get(path).execute().returnContent().asString()
      val depth: BitfinexDepth = parse(json).extract[BitfinexDepth]

      val asks = depth.asks map { o => LimitAskOrder(0, MarketNames.BITFINEX_ID, o.timestamp.toLong, from, to, o.amount.toDouble, o.price.toDouble)}
      val bids = depth.bids map { o => LimitBidOrder(0, MarketNames.BITFINEX_ID, o.timestamp.toLong, from, to, o.amount.toDouble, o.price.toDouble)}

      t = asks ++ bids
    } catch {
      case e: Throwable =>
        t = List[LimitOrder]()
    }
    t
  }

  private def pair2path = (from, to) match {
    case (USD, BTC) => "btcusd"
    case (BTC, USD) => "btcusd"
  }
}
