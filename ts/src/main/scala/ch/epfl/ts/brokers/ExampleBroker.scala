package ch.epfl.ts.brokers

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.{Order, ConfirmRegistration, Register}
import akka.actor.{ActorLogging, Props, ActorRef}
import ch.epfl.ts.engine.{GetWalletFunds, Wallet}

/**
 * Created by sygi on 03.04.15.
 */
class ExampleBroker extends Component with ActorLogging { //TODO(sygi): println -> log.debug
  var mapping = Map[Long, ActorRef]()
  override def receiver: PartialFunction[Any, Unit] = {
    case Register(id) => {
      println("Broker: registration of agent " + id)
      println("with ref: " + sender())
      if (mapping.get(id) != None){
        println("Duplicate Id")
        //TODO(sygi): reply to the trader that registration failed
        //TODO(sygi): return?
      }
      mapping = mapping + (id -> sender())
      context.actorOf(Props[Wallet], "wallet" + id)
      sender() ! ConfirmRegistration
    }
    case GetWalletFunds(uid) => { //TODO(sygi): check if trader asks for his wallet
      println("Broker got a get wallet fund request")
      context.child("wallet" + uid) match {
        case Some(walletActor) => walletActor ! GetWalletFunds(uid)
        case None => println("Broker: No such wallet")
      }
    }
    case o: Order => {
      println("Broker received order")

    }
    case p => println("Broker: received unknown " + p)
  }
}
