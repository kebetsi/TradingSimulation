package ch.epfl.ts.first

import ch.epfl.ts.data.Transaction
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

protected[first] class TranscationPersistanceActor(p: Persistance[Transaction])
  extends PersistanceActor[Transaction] (p: Persistance[Transaction]) 