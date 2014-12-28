package ch.epfl.ts.component.utils

import akka.actor.{Actor, ActorRef}

import scala.reflect.ClassTag

case class BatchSize(size: Int)

class BatcherActor[T: ClassTag](s: Int, dest: List[ActorRef]) extends Actor {
  val clazz = implicitly[ClassTag[T]].runtimeClass

  var size = s
  var batch: List[T] = List()

  def receive = {
    case b: BatchSize => size = b.size

    case d if clazz.isInstance(d) => {
      batch = d.asInstanceOf[T] :: batch
      
      if (batch.size >= size) {
        dest.map { x => x ! batch.reverse }
        batch = List()
      }
    }

    case _ =>
  }
}