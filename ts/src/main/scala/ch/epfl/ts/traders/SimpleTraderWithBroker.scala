package ch.epfl.ts.traders

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import ch.epfl.ts.data.ConfirmRegistration
import ch.epfl.ts.data.Currency.CHF
import ch.epfl.ts.data.Currency.Currency
import ch.epfl.ts.data.Currency.USD
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.MarketOrder
import ch.epfl.ts.data.Order
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Register
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.engine.{GetWalletFunds, WalletConfirm, FundWallet, WalletFunds, ExecutedAskOrder, AcceptedOrder, RejectedOrder}
import ch.epfl.ts.engine.ExecutedBidOrder
import ch.epfl.ts.engine.Wallet

/**
 * SimpleTraderWithBroker companion object
 */
object SimpleTraderWithBroker extends TraderCompanion {
  type ConcreteTrader = SimpleTraderWithBroker
  override protected val concreteTraderTag = scala.reflect.classTag[SimpleTraderWithBroker]
  
  override def strategyRequiredParameters = Map()
}

/**
 * Dummy broker-aware trader.
 */
class SimpleTraderWithBroker(uid: Long, parameters: StrategyParameters)
    extends Trader(uid, parameters)
    with ActorLogging {
  
  // Allows the usage of ask pattern in an Actor
  import context.dispatcher

  override def companion = SimpleTraderWithBroker

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
    case WalletFunds(uid, funds: Wallet.Type) => {
      log.debug("TraderWithB: money I have: ")
      for(i <- funds.keys) yield {log.debug(i + " -> " + funds.get(i))}
    }

    case WalletConfirm(tid) => {
      if (uid != tid)
        log.error("TraderWithB: Broker replied to the wrong trader")
      log.debug("TraderWithB: Got a wallet confirmation")
    }

    case _: ExecutedAskOrder => {
      log.debug("TraderWithB: Got an executed order")
    }
    case _: ExecutedBidOrder => {
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

  override def init = {
    log.debug("TraderWithB received startSignal")
    send(Register(uid))
  }
}
