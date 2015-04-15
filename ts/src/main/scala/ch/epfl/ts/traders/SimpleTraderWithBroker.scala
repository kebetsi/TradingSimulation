package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data._
import akka.actor.{ActorLogging, ActorRef}
import ch.epfl.ts.data.Currency._
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask
import ch.epfl.ts.engine.{GetWalletFunds, WalletConfirm, FundWallet, WalletFunds, ExecutedOrder, AcceptedOrder, RejectedOrder}
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Register
import ch.epfl.ts.data.ConfirmRegistration
import akka.util.Timeout

/**
 * Dummy broker-aware trader.
 */
class SimpleTraderWithBroker(uid: Long) extends Component with ActorLogging{
  import context.dispatcher //allows the usage of ask pattern in an Actor
  val l = context.system.log
  var broker: ActorRef = null
  var registered = false
  var oid = 1L
  override def receiver = {
    case q: Quote => {
      log.debug("TraderWithB receided a quote: " + q)
    }
    case ConfirmRegistration => {
      broker = sender()
      registered = true
      log.debug("TraderWithB: Broker confirmed")
    }
    case WalletFunds(uid, funds :Map[Currency, Double]) => {
      log.debug("TraderWithB: money I have: ")
      for(i <- funds.keys) yield {log.debug(i + " -> " + funds.get(i))}
    }

    case WalletConfirm(tid) => {
      if (uid != tid)
        log.error("TraderWithB: Broker replied to the wrong trader")
      log.debug("TraderWithB: Got a wallet confirmation")
    }

    case _: ExecutedOrder => {
      log.debug("TraderWithB: Got an executed order")
    }

    case 'sendTooBigOrder => {
      val order = MarketBidOrder(oid, uid, System.currentTimeMillis(), CHF, USD, 1000.0, 100000.0)
      placeOrder(order)
      oid = oid + 1
    }
    case 'sendMarketOrder => {
      val order = MarketBidOrder(oid, uid, System.currentTimeMillis(), CHF, USD, 3.0, 14.0)
      placeOrder(order)
      oid = oid + 1
    }
    case 'addFunds => {
      log.debug("TraderWithB: trying to add 100 bucks")
      send(FundWallet(uid, USD, 100))
    }
    case 'knowYourWallet => {
      send(GetWalletFunds(uid))
    }
    case p => {
      println("TraderWithB: received unknown: " + p)
    }
  }

  def placeOrder(order: MarketOrder) = {
    implicit val timeout = new Timeout(500 milliseconds)
    val future = (broker ? order).mapTo[Order]
    future onSuccess {
      case _: AcceptedOrder => log.debug("TraderWithB: order placement succeeded")
      case _: RejectedOrder => log.debug("TraderWithB: order failed")
      case _ => log.debug("TraderWithB: unknown order response")
    }
    future onFailure {
      case p => log.debug("Wallet command failed: " + p)
    }
  }

  override def start = {
    log.debug("TraderWithB received startSignal")
    send(Register(uid))
  }
}