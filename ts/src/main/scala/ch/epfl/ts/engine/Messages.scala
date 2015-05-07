package ch.epfl.ts.engine

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Order
import akka.actor.ActorRef

/*
 * Definition of the Simulator's internal messages.
 */


/* *****************************
 * Order
 */

/* Answer */
case class AcceptedOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends Order

object AcceptedOrder {
  def apply(o: Order): AcceptedOrder = AcceptedOrder(o.oid, o.uid, o.timestamp, o.whatC, o.withC, o.volume, o.price)
}

case class RejectedOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends Order

object RejectedOrder {
  def apply(o: Order): RejectedOrder = RejectedOrder(o.oid, o.uid, o.timestamp, o.whatC, o.withC, o.volume, o.price)
}
case class ExecutedBidOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends Order

case class ExecutedAskOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends Order

object ExecutedBidOrder {
  def apply(o: Order,price:Double): ExecutedBidOrder = ExecutedBidOrder(o.oid, o.uid, o.timestamp, o.whatC, o.withC, o.volume, price)
}
object ExecutedAskOrder {
  def apply(o: Order,price:Double): ExecutedAskOrder = ExecutedAskOrder(o.oid, o.uid, o.timestamp, o.whatC, o.withC, o.volume, price)
}
//TODO(sygi): change this messages and actually use them to communicate broker -> trader


/* *****************************
 * Wallet
 */
abstract class WalletState(val uid: Long)

/* Getter */

/**
 * @param ref To enable using the ask pattern while keeping reliable verification of the GetWalletFund sender
 *            Should be a reference to the trader asking for the funds.
 */
case class GetWalletFunds(override val uid: Long,ref:ActorRef) extends WalletState(uid)

case class GetWalletAllOrders(override val uid: Long) extends WalletState(uid)

case class GetWalletOpenOrder(override val uid: Long) extends WalletState(uid)

case class GetWalletClosedOrder(override val uid: Long) extends WalletState(uid)

case class GetWalletCanceledOrder(override val uid: Long) extends WalletState(uid)

/* Answers */
case class WalletFunds(override val uid: Long, f: Wallet.Type) extends WalletState(uid)

case class WalletConfirm(override val uid: Long) extends WalletState(uid)
case class WalletInsufficient(override val uid: Long) extends WalletState(uid)

case class WalletAllOrders(override val uid: Long, opO: List[Order], clO: List[Order], caO: List[Order]) extends WalletState(uid)

case class WalletOpenOrders(override val uid: Long, opO: List[Order]) extends WalletState(uid)

case class WalletClosedOrders(override val uid: Long, clO: List[Order]) extends WalletState(uid)

case class WalletCanceledOrders(override val uid: Long, caO: List[Order]) extends WalletState(uid)

/* Actions */
case class FundWallet(override val uid: Long, c: Currency, q: Double) extends WalletState(uid)
//TODO(sygi): remove unnecessary messages

/* *****************************
 * Matcher
 */
abstract class MatcherState()

/* Getter */
case class GetMatcherOrderBook(count: Int) extends MatcherState

/* Answers */
case class MatcherOrderBook(bids: List[Order], asks: List[Order]) extends MatcherState