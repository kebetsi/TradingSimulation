package ch.epfl.ts.first


import ch.epfl.ts.data.{Transaction, Order}

import scala.reflect.ClassTag
import akka.actor.{Actor, ActorRef}


abstract class DelayerActor extends Actor {}
 
class TransactionDelayer(dest: List[ActorRef]) extends DelayerActor {
  def receive = {
    case d: Transaction => dest.map(x => x ! d)
    case _ =>
  }
}
class OrderDelayer(dest: List[ActorRef]) extends DelayerActor {
  def receive = {
    case d: Order => dest.map(x => x ! d)
    case _ =>
  }
}