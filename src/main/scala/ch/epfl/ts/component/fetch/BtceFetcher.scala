package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ Order, LimitOrder, Transaction, LiveLimitBidOrder, LiveLimitAskOrder, DelOrder }
import net.liftweb.json._
import org.apache.http.client.fluent._
import ch.epfl.ts.data.LiveLimitBidOrder

class BtceTransactionPullFetcher extends PullFetch[Transaction] {
  val btce = new BtceAPI(USD, BTC)
  var count = 100
  var latest = new Transaction(0, 0.0, 0.0, 0, BTC, USD, 0, 0, 0, 0)

  override def interval(): Int = 12000

  override def fetch(): List[Transaction] = {
    val trades = btce.getTrade(count)
    val idx = trades.indexOf(latest)
    count = if (idx < 0) 100 else Math.min(10 * idx, 100)
    latest = if (trades.length == 0) latest else trades.head

    if (idx > 0)
      trades.slice(0, idx).reverse
    else
      trades.reverse
  }
}

class BtceOrderPullFetcher extends PullFetch[Order] {
  val btce = new BtceAPI(USD, BTC)
  var count = 2000
  override def interval(): Int = 12000

  var oldOrders = Map[LimitOrder, Long]()
  var oid = 76543L
  override def fetch(): List[Order] = {
    // acquire currently active orders
    val currentOrders = btce.getDepth(count)
    println("BtceOrderPullFetcher: current orders size: " + currentOrders.size)
//    println("BtceOrderPullFetcher: currentOrders: " + currentOrders)
    // find which are new by computing the difference: newOrders = currentOrders - oldOrders
    val newOrders: List[LimitOrder] = currentOrders.filterNot(oldOrders.keySet)
    println("BtceOrderPullFetcher: new Orders size: " + newOrders.size + ", should be currentOrders - oldOrders.")
//    println("BtceOrderPullFetcher: newOrders: " + newOrders)
    // assign oid to new orders
    val newOrdersWithId: List[LimitOrder] = newOrders.map { x =>
      x match {
        case lb: LiveLimitBidOrder =>
          oid = oid + 1
          LiveLimitBidOrder(oid, x.uid, x.timestamp, x.whatC, x.withC, x.volume, x.price)
        case la: LiveLimitAskOrder =>
          oid = oid + 1
          LiveLimitAskOrder(oid, x.uid, x.timestamp, x.whatC, x.withC, x.volume, x.price)
      }
    }
//    println("BtceOrderPullFetcher: newOrdersWithId: " + newOrdersWithId)

    // find which were deleted (or executed) by computing the difference: deletedOrders = oldOrders - currentOrders
    val deletedOrders: List[LimitOrder] = oldOrders.keySet.filterNot(currentOrders.toSet).toList
    println("BtceOrderPullFetcher: deletedOrders size: " + deletedOrders.size + ", should be oldOrders - currentOrders")
//    println("BtceOrderPullFetcher: deletedOrders: " + deletedOrders)

    // add new orders to currently active orders (will be old orders in the next iteration)
    newOrdersWithId.map { x => oldOrders += (x -> x.oid) }
    // remove deleted orders from currently active orders (will be old orders in the next iteration)
    deletedOrders.map { x => oldOrders -= x }
    println("BtceOrderPullFetcher: updated old orders size: " + oldOrders.size + ", should be oldOrders - deletedOrders + newOrders")
//    println("BtceOrderPullFetcher: updated old orders: " + oldOrders)

    // convert deleted orders into DelOrders
    val delOrders: List[DelOrder] = deletedOrders.map { x => DelOrder(x.oid, x.uid, x.timestamp, x.whatC, x.withC, x.volume, x.price) }
//    println("BtceOrderPullFetcher: DelOrders: "  + delOrders)

    newOrdersWithId ::: delOrders
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
      val path = serverBase + pair + "/trades/" + count
      val json = Request.Get(path).execute().returnContent().asString()
      t = parse(json).extract[List[BTCeCaseTransaction]]
    } catch {
      case _: Throwable => t = List[BTCeCaseTransaction]();
    }

    if (t.length != 0) {
      t.map(f => new Transaction(MarketNames.BTCE_ID, f.price, f.amount, f.date * 1000, BTC, USD, 0, 0, 0, 0))
    } else {
      List[Transaction]()
    }
  }

  def getDepth(count: Int): List[LimitOrder] = {
    var t = List[LimitOrder]()
    try {
      val path = serverBase + pair + "/depth/" + count
      val json = Request.Get(path).execute().returnContent().asString()
      val a = parse(json).extract[Map[String, List[List[Double]]]]

      val asks = a.get("asks") match {
        case Some(l) => l.map { e => LiveLimitAskOrder(0, MarketNames.BTCE_ID, 0L, from, to, e.last, e.head) }
        case _       => List[LimitOrder]()
      }
      val bids = a.get("bids") match {
        case Some(l) => l.map { e => LiveLimitBidOrder(0, MarketNames.BTCE_ID, 0L, from, to, e.last, e.head) }
        case _       => List[LimitOrder]()
      }
      t = asks ++ bids
    } catch {
      case _: Throwable => t = List[LimitOrder]()
    }
    t
  }

  private def pair2path() = (from, to) match {
    case (USD, BTC) => "btc_usd"
    case (BTC, USD) => "btc_usd"
  }
}