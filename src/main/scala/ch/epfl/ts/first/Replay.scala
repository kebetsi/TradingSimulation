package ch.epfl.ts.first

import akka.actor.{Actor, Cancellable, ActorRef}
import scala.concurrent.duration.DurationLong

case class ReplayConfig(initTime: Long, compression: Double)
class Replay[T](p: Persistance[T], dest: List[ActorRef], conf: ReplayConfig) extends Actor {
  import context._
  var started = false
  var schedule: Cancellable = null
  var currentTime = conf.initTime

  def receive = {
    case "Stop" if started => {
      started = false
      schedule.cancel()
    }
    case "Start" if !started => {
      started = true
      schedule = start(conf.compression)
    }
    case "Tick" if sender == self => {
      currentTime += 1
      process
    }
    case r: ReplayConfig => {
      schedule.cancel()
      currentTime = r.initTime
      // TODO: discard waiting objects
      schedule = start(r.compression)
    }
  }
  private def start(compression: Double) = context.system.scheduler.schedule(10 milliseconds, Math.round(compression * 1000) milliseconds, self, "Tick")
  private def process = p.loadBatch(currentTime, currentTime).map(t => dest.map(d => d ! t))
}