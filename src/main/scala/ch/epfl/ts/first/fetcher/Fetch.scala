package ch.epfl.ts.first.fetcher

import ch.epfl.ts.component.Component

import scala.concurrent.duration.DurationInt

protected[first] trait Fetch[T]

/* Direction PULL */
abstract class PullFetch[T] extends Fetch[T] {
  def fetch(): List[T]
  def interval(): Int
}
/* Direction PUSH */
abstract class PushFetch[T] extends Fetch[T] {
  var callback: (T => Unit)
}

/* Actor implementation */
protected[first] class PullFetchComponent[T](f: PullFetch[T]) extends Component {
  import context._
  private[this] case class Fetch()
  system.scheduler.schedule(0 milliseconds, f.interval() milliseconds, self, Fetch)
  def receiver = {
    // pull and send to each listener
    case Fetch => f.fetch().map( t => sender[T](t))
  }
}

/* Actor implementation */
protected[first] class PushFetchComponent[T] extends Component {
  def receiver = {
    case _ =>
  }
  def callback(data: T) = sender(data)
}