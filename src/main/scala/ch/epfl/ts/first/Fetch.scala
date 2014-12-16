package ch.epfl.ts.first

import akka.actor.{Actor, ActorRef}
import ch.epfl.ts.data.{Transaction, Order}
import scala.concurrent.duration.DurationInt

protected[first] trait Fetch[T]

/* Direction PULL */
abstract class PullFetch[T] extends Fetch[T] {
  def fetch(): List[T]
  def interval(): Int
}
/* Direction PUSH */
abstract class PushFetch[T] extends Fetch[T] {
  var dest = List[ActorRef]()
  def setDest(d: List[ActorRef]) {
    dest = d;
  }
  def submitData(t: T) {
    dest.map(_ ! t)
  }
}

/* Actor implementation */
protected[first] class PullFetchActor[T](f: PullFetch[T], dest: List[ActorRef]) extends Actor {
  import context._
  private[this] case class Fetch()
  system.scheduler.schedule(0 milliseconds, f.interval() milliseconds, self, Fetch)
  override def receive = {
    // pull and send to each listener
    case Fetch => f.fetch().map( t => dest.map(_ ! t))
  }
}

/* Actor implementation */
protected[first] class PushFetchActor[T](f: PushFetch[T], dest: List[ActorRef]) extends Actor {
  override def receive = {
    case _ => f.setDest(dest)
  }
}