package ch.epfl.ts.first

import scala.reflect.ClassTag
import akka.actor.{Actor, ActorRef}

protected[first] class DelayerActor[OutType : ClassTag] (dest: List[ActorRef]) 
  extends Actor {
  
  val clazz = implicitly[ClassTag[OutType]].runtimeClass
  
  def receive = {
    case d if clazz.isInstance(d) => dest.map(_ ! d)
    case _ => 
  }
}