package ch.epfl.ts.component

import akka.actor.{ActorRef, Actor}

case class StartSignal()
case class ActorRegistration(ar: ActorRef, ct: Class[_])

trait Receiver extends Actor{
  def receive: PartialFunction[Any, Unit]
  def sender[T](t: T): Unit
}

trait Component extends Receiver {
  var dest = Map[Class[_], List[ActorRef]]()

  final def componentReceive = PartialFunction[Any, Unit] {
    case ActorRegistration(ar, ct) => dest = dest + (ct -> dest.getOrElse(ct, List(ar)))
    case StartSignal => println("start now") //How do we implement that?
  }

  abstract def receiver: PartialFunction[Any, Unit]
  final def receive = componentReceive orElse receiver

  final def sender[T](t: T): Unit = {
    dest.get(t.getClass) match {
      case Some(l) => l.map(_ ! t)
      case None =>
    }
  }
}


