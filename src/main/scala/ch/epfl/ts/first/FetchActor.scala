package ch.epfl.ts.first

import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala

protected[first] class PullFetchActor[T](f: PullFetch[T], dest: List[ActorRef]) extends Actor {
  import context._
  private[this] case class Fetch()
  
  system.scheduler.schedule(0 milliseconds, f.interval() milliseconds, self, Fetch)
  
  println ("heere")
  
  override def receive = {
    case Fetch => f.fetch().map( t => dest.map(_ ! t))
  }
}

/**
 * Don't use for now
 */
protected[first] class PushFetchActor[T](f: PushFetch[T], dest: ActorRef) extends Actor {
  override def receive = {
    case _ => 
  }
  
}