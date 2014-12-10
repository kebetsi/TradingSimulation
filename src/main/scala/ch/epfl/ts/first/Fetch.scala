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
//abstract class TransactionPullFetch extends PullFetch[Transaction]
//abstract class OrderPullFetch extends PullFetch[Order]

/* Direction PUSH */
abstract class PushFetch[T] (callback: T => Unit) extends Fetch[T]
//abstract class TransactionPushFetch(callback: Transaction => Unit) extends PushFetch[Transaction](callback)
//abstract class OrderPushFetch(callback: Order => Unit) extends PushFetch[Order](callback)


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
/*
protected[first] class TransactionPullFetchActor(f: PullFetch[Transaction], dest: List[ActorRef])
  extends PullFetchActor[Transaction](f, dest)
protected[first] class OrderPullFetchActor(f: PullFetch[Order], dest: List[ActorRef])
  extends PullFetchActor[Order](f, dest)*/

/* Actor implementation */
protected[first] class PushFetchActor[T](f: PushFetch[T], dest: ActorRef) extends Actor {
  override def receive = {
    case _ =>
  }
}
