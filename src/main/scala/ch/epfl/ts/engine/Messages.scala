package ch.epfl.ts.engine

import ch.epfl.ts.data.Currency.Currency

/*
 * Definition of the Simulator's internal messages.
 */

abstract class EngineMessage()

/* *****************************
 * Transactions
 */
case class EngineTransaction() extends EngineMessage

/* *****************************
 * Orders
 */
abstract class EngineOrder(val uid: Long, val whatC: Currency, val price: Double, val quantity: Double, val withC: Currency) extends EngineMessage
case class BidOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends EngineOrder(uid, whatC, price, quantity, withC)
case class AskOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends EngineOrder(uid, whatC, price, quantity, withC)
case class RejectedOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends EngineOrder(uid, whatC, price, quantity, withC)
case class AcceptedOrder(override val uid: Long, override val whatC: Currency, override val price: Double, override val quantity: Double, override val withC: Currency) extends EngineOrder(uid, whatC, price, quantity, withC)


/* *****************************
 * Wallet
 */
abstract class WalletState(val uid: Long) extends EngineMessage

/* Getter */
case class GetWalletFunds(override val uid: Long) extends WalletState(uid)
case class GetWalletAllOrders(override val uid: Long) extends WalletState(uid)
case class GetWalletOpenOrder(override val uid: Long) extends WalletState(uid)
case class GetWalletClosedOrder(override val uid: Long) extends WalletState(uid)
case class GetWalletCanceledOrder(override val uid: Long) extends WalletState(uid)

/* Answers */
case class WalletFunds(override val uid: Long, f: Map[Currency, Double]) extends WalletState(uid)
case class WalletAllOrders(override val uid: Long, opO: List[EngineOrder], clO: List[EngineOrder], caO: List[EngineOrder]) extends WalletState(uid)
case class WalletOpenOrders(override val uid: Long, opO: List[EngineOrder]) extends WalletState(uid)
case class WalletClosedOrders(override val uid: Long, clO: List[EngineOrder]) extends WalletState(uid)
case class WalletCanceledOrders(override val uid: Long, caO: List[EngineOrder]) extends WalletState(uid)

/* Actions */
case class FundWallet(override val uid: Long, c: Currency, q: Double) extends WalletState(uid)



/* *****************************
 * Matcher
 */
abstract class MatcherState() extends EngineMessage

/* Getter */
case class GetMatcherOrderBook(count: Int) extends MatcherState

/* Answers */
case class MatcherOrderBook(bids: List[EngineOrder], asks: List[EngineOrder]) extends MatcherState