package ch.epfl.ts.component.replay

import akka.actor.{Actor, ActorRef, Cancellable}
import ch.epfl.ts.component.persist.Persistance

import scala.concurrent.duration.DurationLong

case class ReplayConfig(initTimeMs: Long, compression: Double)
class Replay[T](p: Persistance[T], dest: List[ActorRef], conf: ReplayConfig) extends Actor {
  import context._
  var started = false
  var schedule: Cancellable = null
  var currentTime = conf.initTimeMs

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
      process
      currentTime += 1000
    }
    case r: ReplayConfig => {
      schedule.cancel()
      currentTime = r.initTimeMs
      // TODO: discard waiting objects
      schedule = start(r.compression)
    }
  }
  private def start(compression: Double) = context.system.scheduler.schedule(10 milliseconds, Math.round(compression * 1000) milliseconds, self, "Tick")
  private def process = p.loadBatch(currentTime, currentTime + 999).map(t => dest.map(d => d ! t))
}