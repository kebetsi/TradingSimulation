package ch.epfl.ts.first

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging


class InStage[OutType] (dest: List[ActorRef], 
    fetch: Fetch[OutType], persist: Persistance[OutType]
  ) extends Actor {
	
  val as = context.system
  val log = Logging(context.system, this)
  
  
  // Actor Fetcher
  val fetchA: ActorRef = if (fetch.isInstanceOf[PullFetch[OutType]]) {
    as.actorOf(Props(classOf[PullFetchActor[OutType]], fetch), "/instage/fetcher")
  } else if (fetch.isInstanceOf[PushFetch[OutType]]) {
    as.actorOf(Props(classOf[PushFetchActor[OutType]], fetch), "/instage/fetcher")
  } else {
    throw new RuntimeException("Type Mismatch")
  }
  
  
  // Actor Persister
  
  
  // Actor Replay
  
  
  // Actor Delayer
  
  
  
  def receive = {
  	case _ ⇒ log.info("received unknown message")
  }
  
  
}	