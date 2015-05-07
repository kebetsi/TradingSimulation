package ch.epfl.ts.component.replay

import akka.actor.Cancellable
import ch.epfl.ts.component.persist.Persistance
import ch.epfl.ts.component.Component

import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.reflect.ClassTag

case class ReplayConfig(initTimeMs: Long, compression: Double)

class Replay[T: ClassTag](p: Persistance[T], conf: ReplayConfig) extends Component {
  import context._
  case object Tick

  var schedule: Cancellable = null
  var currentTime = conf.initTimeMs

  override def receiver = {
    case Tick if sender == self =>
      process()
      currentTime += 1000
    case r: ReplayConfig =>
      schedule.cancel()
      currentTime = r.initTimeMs
      // TODO: discard waiting objects
      schedule = startScheduler(r.compression)
    case _ =>
  }

  private def startScheduler(compression: Double) = {
    context.system.scheduler.schedule(10 milliseconds, Math.round(compression * 1000) milliseconds, self, Tick)
  }

  override def stop = {
    schedule.cancel()
  }

  override def start = {
    schedule = startScheduler(conf.compression)
  }

  private def process() = send[T](p.loadBatch(currentTime, currentTime + 999))
}