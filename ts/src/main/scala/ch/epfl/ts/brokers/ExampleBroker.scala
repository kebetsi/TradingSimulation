package ch.epfl.ts.brokers

import akka.actor.{ActorLogging, Props, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import ch.epfl.ts.component.Component
import ch.epfl.ts.data._
import ch.epfl.ts.engine._
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.engine.WalletConfirm
import scala.concurrent.duration._
import scala.language.postfixOps
import ch.epfl.ts.data.{Order, MarketBidOrder}
import ch.epfl.ts.engine.WalletConfirm
import ch.epfl.ts.data.Register
import ch.epfl.ts.engine.FundWallet
import ch.epfl.ts.engine.WalletFunds
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.engine.WalletInsufficient
import ch.epfl.ts.data.ConfirmRegistration
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.engine.WalletConfirm
import ch.epfl.ts.data.Register
import ch.epfl.ts.engine.FundWallet
import scala.Some
import ch.epfl.ts.engine.WalletFunds
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.engine.WalletInsufficient
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.ConfirmRegistration

/**
 * Created by sygi on 03.04.15.
 */
class ExampleBroker extends Component with ActorLogging { //TODO(sygi): println -> log.debug
  import context.dispatcher
  var mapping = Map[Long, ActorRef]()
  override def receiver: PartialFunction[Any, Unit] = {
    case Register(id) => {
      log.debug("Broker: registration of agent " + id)
      log.debug("with ref: " + sender())
      if (mapping.get(id) != None){
        println("Duplicate Id")
        //TODO(sygi): reply to the trader that registration failed
        //TODO(sygi): return?
      }
      mapping = mapping + (id -> sender())
      context.actorOf(Props[Wallet], "wallet" + id)
      sender() ! ConfirmRegistration
    }
    case FundWallet(uid, curr, value) => {
      log.debug("Broker: got a request to fund a wallet")
      val replyTo = sender
      executeForWallet(uid, FundWallet(uid, curr, value), {
        case WalletConfirm(uid) => {
          println("Broker: Wallet confirmed")
          replyTo ! WalletConfirm(uid)
        }
        case WalletInsufficient(uid) => {
          println("Broker: insufficient funds")
          replyTo ! WalletInsufficient(uid)
        }
        case p => println("FundWallet: Unexpected message " + p)
      })
    }
    case GetWalletFunds(uid) => { //TODO(sygi): check if trader asks for his wallet
      log.debug("Broker: got a get show wallet request")
      val replyTo = sender
      executeForWallet(uid, GetWalletFunds(uid), {
        case w: WalletFunds => {
          replyTo ! w
        }
      })
    }

    //TODO(sygi): refactor charging the wallet/placing an order
    case o: MarketBidOrder => {
      log.debug("Broker: received order")
      val replyTo = sender
      val uid = o.chargedTraderId()
      val placementCost = o.costValue()
      val costCurrency = o.costCurrency()
      executeForWallet(uid, FundWallet(uid, costCurrency, -placementCost), {
        case WalletConfirm(uid) => {
          send(o)
          replyTo ! WalletConfirm(uid) //means: order placed
          log.debug("Broker: Wallet confirmed")
        }
        case WalletInsufficient(uid) => {
          replyTo ! WalletInsufficient(uid)
          log.debug("Broker: insufficient funds")
        }
        case _ => log.debug("Unexpected message")
      })
    }

    case Transaction(mid, price, volume, timestamp, whatC, withC, buyerId, buyOrderId, sellerId, sellOrderId) => {
      log.debug("Broker: received transaction: " + buyerId + " " + sellerId)
      if (mapping.contains(buyerId)){
        //TODO(sygi): refactor charging wallet
        val replyTo = mapping.getOrElse(buyerId, null)
        executeForWallet(buyerId, FundWallet(buyerId, whatC, volume), { //TODO(sygi): this should not be volume, but what?
          case WalletConfirm(uid) => {
            log.debug("Broker: Transaction executed")
            replyTo ! WalletConfirm(uid) //TODO(sygi): change to some better information (or don't inform at all, as everybody gets Transaction)
          }
          case p => log.debug("Broker: A wallet replied with an unexpected message: " + p)
        })
      }
      //TODO(sygi): do the same with the seller
    }
    //TODO(sygi): other orders
    case o: Order => {
      println("Broker received order")

    }
    case p => println("Broker: received unknown " + p)
  }

  def executeForWallet(uid: Long, question: WalletState, f: PartialFunction[Any, Unit]) = {
    context.child("wallet" + uid) match {
      case Some(walletActor) => {
        implicit val timeout = new Timeout(100 milliseconds)
        val future = (walletActor ? question).mapTo[WalletState]
        future onSuccess f
        future onFailure {
          case p => println("Wallet command failed: " + p)
        }
      }
      case None => log.debug("Broker: No such wallet")
    }
  }
}
