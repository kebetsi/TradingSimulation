package ch.epfl.ts.first

import ch.epfl.ts.data.{Transaction, Order}

protected[first] trait Fetch[T]

/* Direction PULL */
abstract class PullFetch[T] extends Fetch[T] {
  def fetch(): List[T]
  def interval(): Int
}
abstract class TransactionPullFetch extends PullFetch[Transaction]
abstract class OrderPullFetch extends PullFetch[Order]

/* Direction PUSH */
abstract class PushFetch[T] (callback: T => Unit) extends Fetch[T]
abstract class TransactionPushFetch(callback: Transaction => Unit) extends PushFetch[Transaction](callback)
abstract class OrderPushFetch(callback: Order => Unit) extends PushFetch[Order](callback)