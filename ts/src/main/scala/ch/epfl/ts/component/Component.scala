package ch.epfl.ts.component

import akka.actor.{Actor, ActorRef, ActorSystem}

import scala.reflect.ClassTag

import scala.language.existentials
import scala.collection.mutable.{HashMap => MHashMap}

case object StartSignal
case object StopSignal
case class ComponentRegistration(ar: ActorRef, ct: Class[_], name: String)

final class ComponentBuilder(name: String) {
  type ComponentProps = akka.actor.Props
  val system = ActorSystem(name)
  var graph = Map[ComponentRef, List[(ComponentRef, Class[_])]]()
  var instances = List[ComponentRef]()

  def add(src: ComponentRef, dest: ComponentRef, data: Class[_]) {
    println("Connecting " + src.ar + " to " + dest.ar + " for type " + data.getSimpleName)
    graph = graph + (src -> ((dest, data) :: graph.getOrElse(src, List[(ComponentRef, Class[_])]())))
    src.ar ! ComponentRegistration(dest.ar, data, dest.name)
  }

  def add(src: ComponentRef, dest: ComponentRef) = (src, dest, classOf[Any])

  def start = instances.map(cr => {
    cr.ar ! StartSignal
    println("Sending start Signal to " + cr.ar)
  })
  
  def stop = instances.map { cr => {
    cr.ar ! StopSignal
    println("Sending stop Signal to " + cr.ar)
  } }

  def createRef(props: ComponentProps, name: String) = {
    instances = new ComponentRef(system.actorOf(props, name), props.clazz, name) :: instances
    instances.head
  }
}

class ComponentRef(val ar: ActorRef, val clazz: Class[_], val name: String) {
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
  var dest = MHashMap[Class[_], List[ActorRef]]()
  var destName = MHashMap[String, ActorRef]()
  var stopped = true

  final def componentReceive: PartialFunction[Any, Unit] = {
    case ComponentRegistration(ar, ct, name) =>
      dest += (ct -> (ar :: dest.getOrElse(ct, List())))
      destName += (name -> ar)
      println("Received destination " + this.getClass.getSimpleName + ": from " + ar + " to " + ct.getSimpleName)
    case StartSignal => stopped = false
      receiver(StartSignal)
      println("Received Start " + this.getClass.getSimpleName)
    case StopSignal => context.stop(self)
      receiver(StopSignal)
      println("Received Stop " + this.getClass.getSimpleName)
    case y if stopped => println("Received data when stopped " + this.getClass.getSimpleName + " of type " + y.getClass )
  }

  def receiver: PartialFunction[Any, Unit]

  /* TODO: Dirty hack, componentReceive giving back unmatched to rematch in receiver using a andThen */
  override def receive = componentReceive orElse receiver

  final def send[T: ClassTag](t: T) = dest.get(t.getClass).map(_.map (_ ! t))
  final def send[T](name: String, t: T) = destName.get(name).map(_ ! t)
  final def send[T: ClassTag](t: List[T]) = t.map( elem => dest.get(elem.getClass).map(_.map(_ ! elem)))
  final def send[T](name: String, t: List[T]) = destName.get(name).map(d => t.map(d ! _))
}

