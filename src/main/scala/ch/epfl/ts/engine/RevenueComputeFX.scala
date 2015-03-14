package ch.epfl.ts.engine

import ch.epfl.ts.component.{ Component, StartSignal }
import ch.epfl.ts.data.Transaction
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import ch.epfl.ts.data.Currency

class RevenueComputeFX(traderNames: Map[Long, String]) extends Component {
  //Simple , just to have a functional output
  /*TODO : Change the architecture to use Wallet inside Trader component for strategies 
involving current wallet state. ( example : buy order with 10% of total available fund)*/
  import context._

  case class Wallet(var funds: Map[Currency.Value, Double])

  var wallets = Map[Long, Wallet]()
  var currentTradingPrice: Double = 0.0

  def receiver = {
    case t: Transaction => process(t)
    case _              =>
  }

  def process(t: Transaction) = {
    currentTradingPrice = t.price
    if (traderNames.contains(t.buyerId)) {
      val traderId = t.buyerId
      val buyerWallet = wallets.getOrElse(t.buyerId, Wallet(Map(Currency.USD -> 5000, Currency.EUR -> 0)))
      buyerWallet.funds.get(t.withC) match {
        case Some(v) =>
          val newFund = v - t.volume * t.price
          buyerWallet.funds += (t.withC -> newFund)
        case None =>
          println("You can't trade those currencies")
      }
      buyerWallet.funds.get(t.whatC) match {
        case Some(v) =>
          val newFund = v + t.volume
          buyerWallet.funds += (t.whatC -> newFund)
        case None =>
          println("You can't trade those currencies")

      }
      wallets += (t.buyerId -> buyerWallet)
      displayStats(t.buyerId)

    } else {
      val traderId = t.sellerId
      val sellerWallet = wallets.getOrElse(t.sellerId, Wallet(Map(Currency.USD -> 5000, Currency.EUR -> 0)))
      sellerWallet.funds.get(t.withC) match {
        case Some(v) =>
          val newFund = v + t.volume * t.price
          sellerWallet.funds += (t.withC -> newFund)
        case None =>
          println("You can't trade those currencies")
      }
      sellerWallet.funds.get(t.whatC) match {
        case Some(v) =>
          val newFund = v - t.volume
          sellerWallet.funds += (t.whatC -> newFund)
        case None =>
          println("You can't trade those currencies")

      }
      wallets += (t.sellerId -> sellerWallet)
      displayStats(t.sellerId)

    }
  }

  def displayStats(traderId: Long) = {
    def computeReturnForTransaction: Double = {
      0.01
    }

    val returns = computeReturnForTransaction
    var disp = new StringBuffer()
    disp.append(f"Stats: trading price=$currentTradingPrice%s \n")
    disp.append("Stats: trader: " + traderNames(traderId) + ", cash=" + wallets(traderId).funds + "\n")
    print(disp)

  }
}
