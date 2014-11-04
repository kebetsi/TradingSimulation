package ch.epfl.ts.first

import scala.reflect.ClassTag
import akka.actor.{Actor, ActorRef}

protected[first] class PersistanceActor[OutType : ClassTag] (p: Persistance[OutType]) 
  extends Actor {
  
  val clazz = implicitly[ClassTag[OutType]].runtimeClass
  
  def receive = {
    case d if clazz.isInstance(d) => p.save(d.asInstanceOf[OutType])
    case _ => 
  }
}