package ch.epfl.ts.engine

import akka.actor.{Actor, ActorRef, Props}
import ch.epfl.ts.data.Currency.Currency
import ch.epfl.ts.data.Order

/**
 * Manages multiple wallets
 */
class WalletDispatcher() extends Actor {
  // TODO: Multiple WalletChief Instances

  import context._

  val wc = system.actorOf(Props(classOf[WalletChief]))

  override def receive = {
    case w: WalletState => wc ! w
  }
}

/*
*
* Manages the content of his wallets
*/
class WalletChief extends Actor {
  var wallets = Map[Long, Wallet]()

  override def receive = {
    case GetWalletFunds(uid) => answerGetWalletFunds(uid, sender())
    case GetWalletAllOrders(uid) => answerGetWalletAllOrders(uid, sender())
    case GetWalletOpenOrder(uid) => answerGetWalletOpenOrder(uid, sender())
    case GetWalletClosedOrder(uid) => answerGetWalletClosedOrder(uid, sender())
    case GetWalletCanceledOrder(uid) => answerGetWalletCanceledOrder(uid, sender())
    case FundWallet(uid, c, q) => fundWallet(uid, c, q)
  }

  def answerGetWalletFunds(uid: Long, s: ActorRef): Unit = {
    sender ! new WalletFunds(uid, wallets.get(uid) match {
      case Some(w) => w.funds
      case None => val w = new Wallet(uid); wallets = wallets + (uid -> w); w.funds
    })
  }

  def answerGetWalletAllOrders(uid: Long, s: ActorRef): Unit = {
    wallets.get(uid) match {
      case Some(w) => sender ! new WalletAllOrders(uid, w.openOrders, w.closedOrders, w.canceledOrders)
      case None =>
        val w = new Wallet(uid)
        wallets = wallets + (uid -> w)
        sender ! new WalletAllOrders(uid, w.openOrders, w.closedOrders, w.canceledOrders)
    }
  }

  def answerGetWalletOpenOrder(uid: Long, s: ActorRef): Unit = {
    sender ! WalletOpenOrders(uid, wallets.get(uid) match {
      case Some(w) => w.openOrders
      case None => val w = new Wallet(uid); wallets = wallets + (uid -> w); w.openOrders
    })
  }

  def answerGetWalletClosedOrder(uid: Long, s: ActorRef): Unit = {
    sender ! WalletClosedOrders(uid, wallets.get(uid) match {
      case Some(w) => w.closedOrders
      case None => val w = new Wallet(uid); wallets = wallets + (uid -> w); w.closedOrders
    })
  }

  def answerGetWalletCanceledOrder(uid: Long, s: ActorRef): Unit = {
    sender ! WalletCanceledOrders(uid, wallets.get(uid) match {
      case Some(w) => w.canceledOrders
      case None => val w = new Wallet(uid); wallets = wallets + (uid -> w); w.canceledOrders
    })
  }

  def fundWallet(uid: Long, c: Currency, q: Double): Unit = {
    wallets.get(uid) match {
      case Some(w) => w.funds.get(c) match {
        case Some(cq) => w.funds + (c -> (cq + q))
        case None => w.funds + (c -> q)
      }
      case None =>
        val w = new Wallet(uid)
        w.funds = w.funds + (c -> q)
        wallets = wallets + (uid -> w)
    }
  }
}


/*
* Represents a wallet
* @param userId
*/
sealed class Wallet(userId: Long) {
  var openOrders: List[Order] = Nil
  var closedOrders: List[Order] = Nil
  var canceledOrders: List[Order] = Nil

  var funds: Map[Currency, Double] = Map[Currency, Double]()
}