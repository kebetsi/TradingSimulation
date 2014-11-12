package ch.epfl.ts.first

import ch.epfl.ts.data.{Transaction, Order}

import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala

protected[first] class PullFetchActor[T](f: PullFetch[T], dest: List[ActorRef]) extends Actor {
  import context._
  private[this] case class Fetch()
  system.scheduler.schedule(0 milliseconds, f.interval() milliseconds, self, Fetch)  
  override def receive = {
    // pull and send to each listener
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

protected[first] class TransactionPullFetchActor(f: PullFetch[Transaction], dest: List[ActorRef])
  extends PullFetchActor[Transaction](f, dest)
  
protected[first] class OrderPullFetchActor(f: PullFetch[Order], dest: List[ActorRef])
  extends PullFetchActor[Order](f, dest)