package ch.epfl.ts.component.utils

import ch.epfl.ts.component.Component
import scala.reflect.ClassTag

case class BatchSize(size: Int)

class BatcherComponent[T: ClassTag](var size: Int) extends Component {
  val clazz = implicitly[ClassTag[T]].runtimeClass
  var batch: List[T] = List()

  override def receiver = {
    case bs: BatchSize => size = bs.size
    case d if clazz.isInstance(d) =>
      batch = d.asInstanceOf[T] :: batch
      if (batch.size >= size)
        send(batch.reverse)
    case _ =>
  }
}