package ch.epfl.ts.first

import akka.actor.{Actor, Cancellable, ActorRef}
import scala.concurrent.duration.DurationLong

class Replay[T](p: Persistance[T], dest: List[ActorRef], initTime: Long, compression: Double) extends Actor {
  var started = false
  var schedule: Cancellable = null
  var currentTime = initTime
  def receive = {
    case "Stop" if started => {
      started = false
      schedule.cancel()
    }
    case "Start" if !started => {
      started = true
      schedule = context.system.scheduler.schedule(10 milliseconds, Math.round(compression * 1000) milliseconds, self, "Tick")
    }
    case "Tick" if sender == self => {
      currentTime += 1
      process
    }
  }
  def process: Unit = {
    p.loadBatch(currentTime, currentTime).map(t => dest.map(d => d ! t))
  }
}