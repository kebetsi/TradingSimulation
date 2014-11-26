package ch.epfl.ts.first

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import ch.epfl.ts.benchmark.{Stop, Start}
import ch.epfl.ts.data.StreamObject
import scala.reflect.ClassTag

trait Stage {
  def broadcast[T](mesg: T)
}

class InStage[T <: StreamObject: ClassTag](as: ActorSystem, out: List[ActorRef]) {

  val clazz = implicitly[ClassTag[T]].runtimeClass

  // Persistance
  var persistance: Option[Persistance[T]] = None

  // Fetcher
  var fetcherCreator: Option[List[ActorRef] => ActorRef] = None
  var fetcherInterface: Option[Fetch[T]] = None

  // Delayer
  val delayerActor = if (clazz.getCanonicalName equals "Order") {
    as.actorOf(Props(classOf[OrderDelayer], out), "order-delayer")
  } else {
    as.actorOf(Props(classOf[TransactionDelayer], out), "order-delayer")
  }

  def withPersistance (p: Persistance[T]): InStage[T]= {
    persistance = Option(p)
    this
  }
  def withPersistance(): InStage[T] = {
    // TODO: Auto-select
    throw new Error("Persistance instance autoselect not implemented")
    this
  }
  def withFetcherActor(arC: List[ActorRef] => ActorRef): InStage[T] = {
    fetcherCreator = Option(arC)
    this
  }
  def withFetchInterface(fInter: Fetch[T]): InStage[T] = {
    fetcherInterface = Option(fInter)
    this
  }

  def start: ActorRef = {
    var fA, pA, rA, dA: Option[ActorRef] = None

    persistance match {
      case f: Some[Persistance[_]] => {}
      case None => {}
    }

    fetcherCreator match {
      case f: Some[List[ActorRef] => ActorRef] => {
        pA match {
          case s: Some[ActorRef] => fA = Option(f.get(List(s.get, delayerActor)))
          case _ => fA = Option(f.get(List(delayerActor)))
        }
      }
      case _ => throw new Error("No fetcher defined")
    }
    as.actorOf(Props(classOf[InStageMaster], this, fA, pA, rA, dA)) // what the fuck?! (this)
  }

  class InStageMaster(f: Option[ActorRef], p: Option[ActorRef], r: Option[ActorRef], d: Option[ActorRef])
    extends Actor with Stage {

    override def receive = {
      case t:Stop => broadcast[Stop](t)
      case t:Start => broadcast[Start](t)
    }
    override def broadcast[T](msg: T) = {
      f match { case a: Some[ActorRef] => a.get ! msg case None => }
      p match { case a: Some[ActorRef] => a.get ! msg case None => }
      r match { case a: Some[ActorRef] => a.get ! msg case None => }
      d match { case a: Some[ActorRef] => a.get ! msg case None => }
    }
  }
}