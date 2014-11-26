package ch.epfl.ts.first

import ch.epfl.ts.data.Transaction
import scala.reflect.ClassTag
import akka.actor.{Actor}

/**
 * The Abstraction for the persistance actors
 */
protected[first] class PersistanceActor[T : ClassTag] (p: Persistance[T])
  extends Actor {
  val clazz = implicitly[ClassTag[T]].runtimeClass
  def receive = {
    case d if clazz.isInstance(d) => p.save(d.asInstanceOf[T])
    case _ =>
  }
}

protected[first] class TranscationPersistanceActor(p: Persistance[Transaction])
  extends PersistanceActor[Transaction] (p)