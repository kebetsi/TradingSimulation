package ch.epfl.ts.first

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import akka.event.Logging
import scala.reflect.ClassTag


case class StreamObject()

trait Stage {
  def stop()
}

class InStage[T <: StreamObject: ClassTag](as: ActorSystem, out: List[ActorRef]) {

  val clazz = implicitly[ClassTag[T]].runtimeClass

  // Persistance
  var persistance: Option[Persistance[T]] = None

  // Fetcher
  var fetcherCreator: Option[List[ActorRef] => ActorRef]
  var fetcherInterface: Option[Fetch[T]]

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
      case f: Some[Persistance[T]] => {

      }
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


    as.actorOf(Props(classOf[InStageMaster],fA, pA, rA, dA))
  }

  class InStageMaster(f: Option[ActorRef], p: Option[ActorRef], r: Option[ActorRef], d: Option[ActorRef])
    extends Actor with Stage {

    override def receive = {
      case Stop => stop()
    }
    case class Stop()
    override def stop() = {
      f match { case a: Some[ActorRef] => a.get ! Stop() }
      p match { case a: Some[ActorRef] => a.get ! Stop() }
      r match { case a: Some[ActorRef] => a.get ! Stop() }
      d match { case a: Some[ActorRef] => a.get ! Stop() }
    }
  }


}