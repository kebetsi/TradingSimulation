package ch.epfl.ts.engine

import ch.epfl.ts.data.Currency.Currency

/*
 * Definition of the Simulator's internal messages.
 */

abstract class Message()

/* *****************************
 * Transactions
 */
case class Transaction() extends Message

/* *****************************
 * Orders
 */
abstract class Order(val uid: Long, val whatC: Currency, val price: Double, val quantity: Double, val withC: Currency) extends Message
case class BidOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends Order(uid, whatC, price, quantity, withC)
case class AskOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends Order(uid, whatC, price, quantity, withC)
case class RejectedOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends Order(uid, whatC, price, quantity, withC)
case class AcceptedOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends Order(uid, whatC, price, quantity, withC)


/* *****************************
 * Wallet
 */
abstract class WalletState(val uid: Long) extends Message

/* Getter */
case class GetWalletFunds(override val uid: Long) extends WalletState(uid)
case class GetWalletAllOrders(override val uid: Long) extends WalletState(uid)
case class GetWalletOpenOrder(override val uid: Long) extends WalletState(uid)
case class GetWalletClosedOrder(override val uid: Long) extends WalletState(uid)
case class GetWalletCanceledOrder(override val uid: Long) extends WalletState(uid)

/* Answers */
case class WalletFunds(override val uid: Long, f: Map[Currency, Double]) extends WalletState(uid)
case class WalletAllOrders(override val uid: Long, opO: List[Order], clO: List[Order], caO: List[Order]) extends WalletState(uid)
case class WalletOpenOrders(override val uid: Long, opO: List[Order]) extends WalletState(uid)
case class WalletClosedOrders(override val uid: Long, clO: List[Order]) extends WalletState(uid)
case class WalletCanceledOrders(override val uid: Long, caO: List[Order]) extends WalletState(uid)

/* Actions */
case class FundWallet(override val uid: Long, c: Currency, q: Double) extends WalletState(uid)



/* *****************************
 * Matcher
 */
abstract class MatcherState() extends Message

/* Getter */
case class GetMatcherOrderBook(count: Int) extends MatcherState

/* Answers */
case class MatcherOrderBook(bids: List[Order], asks: List[Order]) extends MatcherState