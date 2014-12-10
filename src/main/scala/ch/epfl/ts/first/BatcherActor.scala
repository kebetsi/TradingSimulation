package ch.epfl.ts.first

import akka.actor.Actor
import akka.actor.ActorRef
import scala.reflect.ClassTag

case class batchSize(size: Int)

class BatcherActor[T : ClassTag](size: Int, dest: List[ActorRef]) extends Actor {
  var s = size
  var batch: List[T] = List()
  val clazz = implicitly[ClassTag[T]].runtimeClass

  def receive = {
    case b: batchSize => s = b.size
    case d if clazz.isInstance(d) => {
      batch = d.asInstanceOf[T] :: batch
      if (batch.size >= s) {
        dest.map { x => x ! batch }
        batch = List()
      }
    }
    case _ =>
  }
}