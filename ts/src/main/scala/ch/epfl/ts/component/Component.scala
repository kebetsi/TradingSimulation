package ch.epfl.ts.component

import akka.actor._

import scala.reflect.ClassTag

import scala.language.existentials
import scala.collection.mutable.{HashMap => MHashMap}
import com.typesafe.config.{ConfigFactory, Config}

case object StartSignal
case object StopSignal
case class ComponentRegistration(ar: ActorRef, ct: Class[_], name: String)

final class ComponentBuilder(myName: String, config: Config) {
  type ComponentProps = akka.actor.Props
  val system = ActorSystem(myName, config)
  var graph = Map[ComponentRef, List[(ComponentRef, Class[_])]]()
  var instances = List[ComponentRef]()

  def this(name: String){
    this(name, ConfigFactory.load())
  }

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
    instances = new ComponentRef(system.actorOf(props, name), props.clazz, name, this) :: instances
    instances.head
  }
}

/** Encapsulates [[akka.actor.ActorRef]] to facilitate connection of components
  */
class ComponentRef(val ar: ActorRef, val clazz: Class[_], val name: String, cb: ComponentBuilder) {
  /** Connects current component to the destination component
    *
    * @param destination the destination component
    * @param types the types of messages that the destination expects to receive
    */
  def ->(destination: ComponentRef, types: Class[_]*) = {
    types.map(cb.add(this, destination, _))
  }

  /** Connects current component to the specified components
    *
    * @param refs the destination components
    * @param types the types of messages that the destination components expect to receive
    */
  def ->(refs: Seq[ComponentRef], types: Class[_]*) = {
    for (ref <- refs; typ <- types) cb.add(this, ref, typ)
  }
}

trait Receiver extends Actor {
  def receive: PartialFunction[Any, Unit]

  def send[T: ClassTag](t: T): Unit
  def send[T: ClassTag](t: List[T]): Unit
}

abstract class Component extends Receiver {
  var dest = MHashMap[Class[_], List[ActorRef]]()
  var stopped = true

  final def componentReceive: PartialFunction[Any, Unit] = {
    case ComponentRegistration(ar, ct, name) =>
      connect(ar, ct, name)
      println("Received destination " + this.getClass.getSimpleName + ": from " + ar + " to " + ct.getSimpleName)
    case StartSignal => stopped = false
      start
      println("Received Start " + this.getClass.getSimpleName)
    case StopSignal => context.stop(self)
      stop
      println("Received Stop " + this.getClass.getSimpleName)
    case y if stopped => println("Received data when stopped " + this.getClass.getSimpleName + " of type " + y.getClass )
  }

  /**
   * Connects two compoenents
   *
   * Normally subclass don't need to override this method.
   * */
  def connect(ar: ActorRef, ct: Class[_], name: String): Unit = {
    dest += (ct -> (ar :: dest.getOrElse(ct, List())))
  }

  /**
   * Starts the component
   *
   * Subclass can override do initialization here
   * */
  def start: Unit = {}

  /**
   * Stops the component
   *
   * Subclass can override do release resources here
   * */
  def stop: Unit = {}

  def receiver: PartialFunction[Any, Unit]

  /* TODO: Dirty hack, componentReceive giving back unmatched to rematch in receiver using a andThen */
  override def receive = componentReceive orElse receiver

  def send[T: ClassTag](t: T) = dest.get(t.getClass).map(_.map (_ ! t)) //TODO(sygi): support superclasses
  def send[T: ClassTag](t: List[T]) = t.map( elem => dest.get(elem.getClass).map(_.map(_ ! elem)))
}
