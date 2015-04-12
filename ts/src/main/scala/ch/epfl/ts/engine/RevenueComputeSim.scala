package ch.epfl.ts.engine

import ch.epfl.ts.data.Transaction
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import ch.epfl.ts.data.Currency

class RevenueComputeSim(traderNames: Map[Long, String],pingIntervalMillis: Int) extends RevenueCompute(traderNames) {
  import context._

/*
 * The revenue compute component for simulation purpose.
 * Display Stats : Display stats of the buyer trader and the seller trader
 * 
 */

  def receiver = {
    case t: Transaction => process(t)
    case _              =>
  }

  def process(t: Transaction) = {
    currentTradingPrice = t.price
    
    // Buyer has more shares but less money
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

     // Seller has more money but less shares
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
    
    displayStats(t.buyerId)
    displayStats(t.sellerId)
  }
  
  



}