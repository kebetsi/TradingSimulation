package ch.epfl.ts.first

import akka.actor.Actor
import ch.epfl.ts.data.{Transaction, Order}

import ch.epfl.ts.component.Component

import scala.reflect.ClassTag

/**
 * Defines the Persistance interface
 * @tparam T
 */
trait Persistance[T] {
  def save(t: T)
  def save(ts: List[T])
  def loadSingle(id: Int) : T
  def loadBatch(startTime: Long, endTime: Long) : List[T]
}

/**
 * The Abstraction for the persistance actors
 */
class PersistanceComponent[T : ClassTag] (p: Persistance[T])
  extends Component {
  val clazz = implicitly[ClassTag[T]].runtimeClass
  def receiver = {
    case d if clazz.isInstance(d) => p.save(d.asInstanceOf[T])
    case _ =>
  }
}
/*
protected[first] class TransactionPersistanceActor(p: Persistance[Transaction])
  extends PersistanceActor[Transaction] (p)
protected[first] class OrderPersistanceActor(p: Persistance[Order])
  extends PersistanceActor[Order] (p)*/