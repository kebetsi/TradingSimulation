package ch.epfl.ts.first


import ch.epfl.ts.data.Transaction

import scala.reflect.ClassTag
import akka.actor.{Actor, ActorRef}


 class DelayerActor[OutType: ClassTag] (dest: List[ActorRef]) 
  extends Actor {
  
  val clazz = implicitly[ClassTag[OutType]].runtimeClass
  
  def receive = {
    case d if clazz.isInstance(d) => dest.map(_ ! d)
    case _ => 
  }
}
 
class TransactionDelayer(dest: List[ActorRef]) extends DelayerActor[Transaction](dest) {}