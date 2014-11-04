package ch.epfl.ts.first

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

class InStage[OutType] (dest: List[ActorRef], 
    fetch: Fetch[OutType], persist: Persistance[OutType]
  ) extends Actor {
  
  val as = context.system
  val log = Logging(context.system, this)
  
  // Actor Delayer
  val dActor = as.actorOf(Props(classOf[DelayerActor[OutType]], dest), "/instage/delayer")
  
  // Actor Persister
  val pActor = as.actorOf(Props(classOf[PersistanceActor[OutType]], persist), "/instage/persiter")
  
  // Actor Fetcher
  val fetchA: ActorRef = if (fetch.isInstanceOf[PullFetch[OutType]]) {
    as.actorOf(Props(classOf[PullFetchActor[OutType]], fetch, List(pActor, dActor)), "/instage/fetcher")
  } else if (fetch.isInstanceOf[PushFetch[OutType]]) {
    as.actorOf(Props(classOf[PushFetchActor[OutType]], fetch, List(pActor, dActor)), "/instage/fetcher")
  } else {
    throw new RuntimeException("Type Mismatch")
  }
  
  
  
  
  
  // Actor Replay
  
  
  
  
  
  def receive = {
  	case _ =>
  }
  
  
}	