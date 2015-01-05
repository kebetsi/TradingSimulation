package ch.epfl.ts.traders

import akka.actor.Actor
import akka.actor.ActorRef
import ch.epfl.ts.data.Transaction

class VwapTrader(dest: List[ActorRef], timeFrameMillis: Long) extends Actor {
  
  
  
  def receive = {
    case t: Transaction => 
    case _ => println("vwapTrader: unknown message received")
  }
}