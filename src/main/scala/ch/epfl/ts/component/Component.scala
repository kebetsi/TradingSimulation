package ch.epfl.ts.component

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

case class StartSignal()

case class StopSignal()

case class ActorRegistration(ar: ActorRef, ct: Class[_])

final class ComponentBuilder(implicit as: ActorSystem) {
  var graph = Map[Actor, List[(Actor, Class[_])]]()
  var instances = Map[Actor, ActorRef]()

  def add(src: Component, dest: Component, data: Class[_]) {
    graph = graph + (src -> graph.getOrElse(src, List[(Actor, Class[_])]((dest, data))))
  }

  def add(src: Component, dest: Component) = (src, dest, classOf[Any])

  def start = {
    instances = Map(graph.foldLeft(List[(Actor)]()) { case (a, (k, v)) => k :: a ::: v.map(e => e._1)}
      .distinct.map { i => (i, as.actorOf(Props(i)))}: _*)
    // Create the connections
    graph.map { case (src, dest) => dest.map(d => {
      instances.get(src).get ! ActorRegistration(instances.get(d._1).get, d._2)
    })
    }
    // Launch the system
    instances.map { case (a, ar) => ar ! StartSignal}
  }
}

trait Receiver extends Actor {
  def receive: PartialFunction[Any, Unit]

  def sender[T](t: T): Unit
}

trait Component extends Receiver {

  var stopped = true

  final def componentReceive = PartialFunction[Any, Unit] {
    case ActorRegistration(ar, ct) => dest = dest + (ct -> dest.getOrElse(ct, List(ar)))
    case StartSignal => stopped = false
    case StopSignal => context.stop(self)
    case _ if stopped => // discard
  }

  def receiver: PartialFunction[Any, Unit]

  final def receive = componentReceive orElse receiver

  final def sender[T](t: T): Unit = {
    dest.get(t.getClass) match {
      case Some(l) => l.map(_ ! t)
      case None =>
    }
  }

  var dest = Map[Class[_], List[ActorRef]]()

  def addDestination(destination: Component, data: Class[_])(implicit cb: ComponentBuilder) = {
    cb.add(this, destination, data)
  }
}


