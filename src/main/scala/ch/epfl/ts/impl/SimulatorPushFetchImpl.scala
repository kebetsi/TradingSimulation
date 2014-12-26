package ch.epfl.ts.impl

import akka.actor.Actor
import akka.actor.ActorRef

class SimulatorPushFetchImpl[T](source: List[ActorRef], dest: List[ActorRef]) extends Actor {

  def receive = {
    case "Start" => source.map { _ ! self }
    case t: T    => dest.map { _ ! t }
  }
}