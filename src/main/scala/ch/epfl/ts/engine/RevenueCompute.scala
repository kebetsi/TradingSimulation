package ch.epfl.ts.engine

import ch.epfl.ts.component.{ Component, StartSignal }
import ch.epfl.ts.data.Transaction

import scala.concurrent.duration.DurationInt

class RevenueCompute(pingIntervalMillis: Int, traderNames: Map[Long, String]) extends Component {

  import context._

  case class Tick()
  case class Wallet(var shares: Double = 0.0, var money: Double = 0.0)

  var wallets = Map[Long, Wallet]()
  var oldTradingPrice: Double = 0.0
  var currentTradingPrice: Double = 0.0

  def receiver = {
    case StartSignal()  => startScheduler()
    case Tick()         => displayStats
    case t: Transaction => process(t)
    case _              =>
  }

  def process(t: Transaction) = {
    currentTradingPrice = t.price
    // buyer has more shares but less money
    val buyerWallet = wallets.getOrElse(t.buyerId, Wallet())
    buyerWallet.money = buyerWallet.money - t.volume * t.price
    buyerWallet.shares = buyerWallet.shares + t.volume
    wallets += (t.buyerId -> buyerWallet)

    // seller has more money but less shares
    val sellerWallet = wallets.getOrElse(t.sellerId, Wallet())
    sellerWallet.money = sellerWallet.money + t.volume * t.price
    sellerWallet.shares = sellerWallet.shares - t.volume
    wallets += (t.sellerId -> sellerWallet)
  }

  def displayStats = {
    val changeInPrice: Double = computePriceEvolution
    var disp = new StringBuffer()
    //    println(f"Stats: trading price=$currentTradingPrice%s, change=$changeInPrice%.2f %%")
    disp.append(f"Stats: trading price=$currentTradingPrice%s, change=$changeInPrice%.4f %% \n")
    //    wallets.keys.map { x => println("Stats: trader: " + traderNames(x) + ", cash=" + wallets(x).money + ", shares=" + wallets(x).shares + " Revenue=" + (wallets(x).money + wallets(x).shares * currentTradingPrice))}
    wallets.keys.map { x => disp.append("Stats: trader: " + traderNames(x) + ", cash=" + wallets(x).money + ", shares=" + wallets(x).shares + " Revenue=" + (wallets(x).money + wallets(x).shares * currentTradingPrice) + "\n") }
    print(disp)
    oldTradingPrice = currentTradingPrice
  }

  def startScheduler() = system.scheduler.schedule(0 milliseconds, pingIntervalMillis milliseconds, self, Tick())

  def computePriceEvolution: Double = {
    (currentTradingPrice - oldTradingPrice) / oldTradingPrice
  }

}