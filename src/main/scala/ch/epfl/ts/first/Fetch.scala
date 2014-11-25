package ch.epfl.ts.first

import akka.actor.{Actor, ActorRef}
import ch.epfl.ts.data.{Transaction, Order}

protected[first] trait Fetch[T] {
	
}

abstract class PullFetch[T] extends Fetch[T] {
  def fetch(): List[T]
  def interval(): Int
}
abstract class TransactionPullFetch extends PullFetch[Transaction]
abstract class OrderPullFetch extends PullFetch[Order]

abstract class PushFetch[T] (callback: T => Unit) extends Fetch[T]