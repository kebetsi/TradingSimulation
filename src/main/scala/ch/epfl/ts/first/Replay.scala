package ch.epfl.ts.first

import scala.reflect.ClassTag
import akka.actor.Actor

protected[first] abstract class Replay[T : ClassTag] extends Actor {
  
  val clazz = implicitly[ClassTag[T]].runtimeClass
  
  def receive = {
    case d if clazz.isInstance(d) => process(d.asInstanceOf[T])
    case _ => 
  }

  def process (data: T): Unit
}