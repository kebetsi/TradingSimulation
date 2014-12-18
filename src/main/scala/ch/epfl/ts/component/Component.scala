package ch.epfl.ts.component

import akka.actor._

protected[component] case class StartSignal()

protected[component] case class StopSignal()

protected[component] case class ComponentRegistration(ar: ActorRef, ct: Class[_])

final class ComponentBuilder(name: String) {
  type ComponentProps = Props
  val system = ActorSystem(name)
  var graph = Map[ComponentRef, List[(ComponentRef, Class[_])]]()
  var instances = List[ComponentRef]()

  def add(src: ComponentRef, dest: ComponentRef, data: Class[_]) {
    graph = graph + (src -> graph.getOrElse(src, List[(ComponentRef, Class[_])]((dest, data))))
  }

  def add(src: ComponentRef, dest: ComponentRef) = (src, dest, classOf[Any])

  def start = {
    graph.map { case (src, dest) => dest.map(d => src.ar ! ComponentRegistration(d._1.ar, d._2))}
    instances.map(_.ar ! StartSignal)
  }

  def createRef(props: ComponentProps) = {
    instances = new ComponentRef(system.actorOf(props), props.clazz) :: instances
    instances.head
  }

  def createRef(props: ComponentProps, name: String) = {
    instances = new ComponentRef(system.actorOf(props, name), props.clazz) :: instances
    instances.head
  }
}

class ComponentRef(val ar: ActorRef, val clazz: Class[_]) {
  // TODO: Verify clazz <: Component
  def addDestination(destination: ComponentRef, data: Class[_])(implicit cb: ComponentBuilder) = {
    cb.add(this, destination, data)
  }
}


trait Receiver extends Actor {
  def receive: PartialFunction[Any, Unit]

  def sender[T](t: T): Unit
}

trait Component extends Receiver {
  var dest = Map[Class[_], List[ActorRef]]()
  var stopped = true

  final def componentReceive = PartialFunction[Any, Unit] {
    case ComponentRegistration(ar, ct) => dest = dest + (ct -> dest.getOrElse(ct, List(ar)))
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
}


