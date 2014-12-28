package ch.epfl.ts.component.fetch


import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{Order, OrderType, Transaction}
import net.liftweb.json._
import org.apache.http.client.fluent._

class BitfinexTransactionPullFetcherComponent(val name: String) extends PullFetchComponent[Transaction](new BitfinexTransactionPullFetcher)

class BitfinexTransactionPullFetcher extends PullFetch[Transaction] {
  val btce = new BitfinexAPI(USD, BTC)
  var count = 2000
  var latest = new Transaction(0.0, 0.0, 0, USD, 0, 0, 0, 0)

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

case class BitfinexCaseTransaction(timestamp: Long, tid: Int, price: String,
  amount: String, exchange: String)

class BitfinexAPI(from: Currency, to: Currency){
  implicit val formats = net.liftweb.json.DefaultFormats
  val serverBase = "https://api.bitfinex.com/v1/"
  val pair = pair2path

  def getInfo() {}

  def getTicker() {}

  def getTrade(count: Int) : List[Transaction] = {
    val path = serverBase + "/trades/" + pair
    val json = Request.Get(path).execute().returnContent().asString()
    val t = parse(json).extract[List[BitfinexCaseTransaction]]

    if (t.length != 0) {
      t.map(f => new Transaction(f.price.toDouble, f.amount.toDouble, f.timestamp, USD, 0, 0, 0, 0))
    } else {
      List[Transaction]()
    }
  }
  def getDepth() { }

  private def pair2path() = (from, to) match {
    case (USD, BTC) => "btcusd"
    case (BTC, USD) => "btcusd"
  }
}
