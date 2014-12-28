package ch.epfl.ts.component

import akka.actor.{Actor, ActorRef, ActorSystem}

import scala.reflect.ClassTag


private[component] case class StartSignal()

private[component] case class StopSignal()

private[component] case class ComponentRegistration(ar: ActorRef, ct: Class[_])


final class ComponentBuilder(name: String) {
  type ComponentProps = akka.actor.Props
  val system = ActorSystem(name)
  var graph = Map[ComponentRef, List[(ComponentRef, Class[_])]]()
  var instances = List[ComponentRef]()

  def add(src: ComponentRef, dest: ComponentRef, data: Class[_]) {
    println("Connecting " + src.ar + " to " + dest.ar + " for type " + data.getSimpleName)
    graph = graph + (src -> ((dest, data) :: graph.getOrElse(src, List[(ComponentRef, Class[_])]())))
    src.ar ! ComponentRegistration(dest.ar, data)
  }

  def add(src: ComponentRef, dest: ComponentRef) = (src, dest, classOf[Any])

  def start = instances.map(cr => {
    cr.ar ! new StartSignal()
    println("Sending start Signal to " + cr.ar)
  })


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

  def send[T: ClassTag](t: T): Unit
  def send[T: ClassTag](t: List[T]): Unit
}

abstract class Component extends Receiver {
  var dest = Map[Class[_], List[ActorRef]]()
  var stopped = true

  final def componentReceive = PartialFunction[Any, Any] {
    case ComponentRegistration(ar, ct) => {
      dest = dest + (ct -> (ar :: dest.getOrElse(ct, List())))
      println("Received destination " + this.getClass.getSimpleName + ": from " + ar + " to " + ct.getSimpleName)
    }
    case s: StartSignal => stopped = false; println("Received Start " + this.getClass.getSimpleName); s
    case s: StopSignal => context.stop(self); println("Received Stop " + this.getClass.getSimpleName); s
    case y if stopped => println("Received data when stopped " + this.getClass.getSimpleName + " of type " + y.getClass )
    case x => x
  }

  def receiver: PartialFunction[Any, Unit]

  /* TODO: Dirty hack, componentReceive giving back unmatched to rematch in receiver using a andThen */
  final override def receive = componentReceive andThen receiver

  final def send[T: ClassTag](t: T): Unit = {
    dest.get(t.getClass) match {
      case Some(l) => l.map(_ ! t)
      case None =>
    }
  }

  final def send[T: ClassTag](t: List[T]): Unit = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    dest.get(clazz) match {
      case Some(l) => l.map(d => t.map(d ! _))
      case None =>
    }
  }
}

