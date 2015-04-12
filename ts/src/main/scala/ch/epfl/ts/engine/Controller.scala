package ch.epfl.ts.engine

import akka.actor.Actor
import ch.epfl.ts.data.Order
import ch.epfl.ts.engine.Actors._


class Controller(wm: WalletManager, me: MatcherEngine) extends Actor {
  type ClientId = Long
  var clients = Map[ClientId, Client]()

  override def receive: Receive = {
    case ro: RejectedOrder => deny(ro)
    case ao: AcceptedOrder => accept(ao)
    case o: Order => verify(o, sender)

    case ws: WalletState => askWalletState(ws, sender)
    case _ => {} // Where is the public data?
  }

  def verify(o: Order, s: Client): Unit = {
    clients = clients + (o.uid -> s)
  }

  def deny(o: Order): Unit = {
    clients.get(o.uid) match {
      case Some(c) => c ! o
      case None =>
    }
  }

  def accept(o: Order): Unit = {
    me ! o
    clients.get(o.uid) match {
      case Some(c) => c ! o
      case None =>
    }
  }

  def askWalletState(ws: WalletState, c: Client): Unit = {
    clients = clients + (ws.uid -> c)
    this.wm ! ws
  }
}
