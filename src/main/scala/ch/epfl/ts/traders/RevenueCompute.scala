package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.component.StartSignal
import scala.concurrent.duration.DurationInt

class RevenueCompute(pingIntervalMillis: Int) extends Component {
  import context._

  case class Tick()
  case class Wallet(var shares: Double = 0.0, var money: Double = 0.0)

  var wallets = Map[Long, Wallet]()
  var tradingPrice: Double = 0.0

  def receiver = {
    case StartSignal()  => startScheduler
    case Tick()         => display
    case t: Transaction => process(t)
    case _              =>
  }

  def process(t: Transaction) = {
    tradingPrice = t.price
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

  def display = {
    println("Stats:")
    wallets.keys.map { x => println("uid: " + x + ", cash=" + wallets(x).money + ", shares=" + wallets(x).shares + " Revenue=" + (wallets(x).money + wallets(x).shares * tradingPrice)) }
    
  }

  def startScheduler = system.scheduler.schedule(0 milliseconds, pingIntervalMillis milliseconds, self, Tick())

}