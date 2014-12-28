package ch.epfl.ts.component.replay

import akka.actor.{Actor, ActorRef, Cancellable}
import ch.epfl.ts.component.{StopSignal, StartSignal, Component}
import ch.epfl.ts.component.persist.Persistance
import ch.epfl.ts.data.{Order, Transaction, Tweet}

import scala.concurrent.duration.DurationLong
import scala.reflect.ClassTag

case class ReplayConfig(initTimeMs: Long, compression: Double)

class TransactionReplay(p: Persistance[Transaction], conf: ReplayConfig) extends Replay[Transaction](p, conf)
class OrderReplay(p: Persistance[Order], conf: ReplayConfig) extends Replay[Order](p, conf)
class TweetReplay(p: Persistance[Tweet], conf: ReplayConfig) extends Replay[Tweet](p, conf)

class Replay[T: ClassTag](p: Persistance[T], conf: ReplayConfig) extends Component {
  import context._
  case class Tick()
  var schedule: Cancellable = null
  var currentTime = conf.initTimeMs

  def receiver = {
    case StopSignal if stopped =>
      schedule.cancel()
    case StartSignal if !stopped =>
      schedule = start(conf.compression)
    case Tick if sender == self =>
      process()
      currentTime += 1000
    case r: ReplayConfig =>
      schedule.cancel()
      currentTime = r.initTimeMs
      // TODO: discard waiting objects
      schedule = start(r.compression)
  }
  private def start(compression: Double) = context.system.scheduler.schedule(10 milliseconds, Math.round(compression * 1000) milliseconds, self, new Tick)
  private def process() = send[T](p.loadBatch(currentTime, currentTime + 999))
}