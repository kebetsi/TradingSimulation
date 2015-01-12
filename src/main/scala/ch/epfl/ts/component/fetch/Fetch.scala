package ch.epfl.ts.component.fetch

import ch.epfl.ts.component.Component
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag
import sun.awt.image.FetcherInfo

trait Fetch[T]

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
class PullFetchComponent[T: ClassTag](f: PullFetch[T]) extends Component {
  import context._
  case object Fetch
  system.scheduler.schedule(0 milliseconds, f.interval() milliseconds, self, Fetch)

  override def receiver = {
    // pull and send to each listener
    case Fetch =>
      println("PullFetchComponent Fetch " + System.currentTimeMillis())
      f.fetch().map(t => send[T](t))
    case _ =>
  }
}

/* Actor implementation */
class PullFetchListComponent[T: ClassTag](f: PullFetch[T]) extends Component {
  import context._
  case object Fetch
  system.scheduler.schedule(0 milliseconds, f.interval() milliseconds, self, Fetch)

  override def receiver = {
    // pull and send to each listener
    case Fetch =>
      println("PullFetchListComponent Fetch " + System.currentTimeMillis())
      send(f.fetch())
    case _ =>
  }
}

/* Actor implementation */
class PushFetchComponent[T: ClassTag] extends Component {
  override def receiver = {
    case _ =>
  }

  def callback(data: T) = send(data)
}