package ch.epfl.ts.first

import ch.epfl.ts.data.Transaction

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

class InStage[OutType] (dest: List[ActorRef], 
    fetch: Fetch[OutType], persist: Persistance[OutType]
  ) extends Actor {
  
  val as = context.system
  val log = Logging(context.system, this)
  
  // Actor Delayer
  //val dActor = as.actorOf(Props(classOf[DelayerActor[OutType]], List(dest)), "instage-delayer")
  val dActor = as.actorOf(Props(classOf[TransactionDelayer], List(dest)), "instage-delayer")
  
  // Actor Persistor
  val pActor = as.actorOf(Props(classOf[TranscationPersistanceActor], persist), "instage-persiter")
  
  // Actor Fetcher
  println(fetch);
  
  /*
  val fetchA: ActorRef = if (fetch.isInstanceOf[TransactionPullFetch]) {
    as.actorOf(Props(classOf[TransactionPullFetchActor], fetch, List(pActor, dActor)), "instage-fetcher")
  } else if (fetch.isInstanceOf[PushFetch[OutType]]) {
    as.actorOf(Props(classOf[PushFetchActor[OutType]], fetch, List(pActor, dActor)), "instage-fetcher")
  } else {
    throw new RuntimeException("Type Mismatch")
  }*/
  
  val fetchA: ActorRef = 
    as.actorOf(Props(classOf[TransactionPullFetchActor], fetch, List(pActor, dActor)), "instage-fetcher")
  
  // Actor Replay
  
  
    
  
  
  
  def receive = {
    case "init" => dActor ! pActor; dActor ! fetchA; pActor ! fetchA
  	case _ =>
  }
  
  
}	