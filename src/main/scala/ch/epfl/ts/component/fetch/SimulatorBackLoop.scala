package ch.epfl.ts.component.fetch

import ch.epfl.ts.component.Component

import scala.reflect.ClassTag

class SimulatorBackLoop[T: ClassTag]() extends Component {
  val clazz = implicitly[ClassTag[T]].runtimeClass
  override def receiver = {
    case t if clazz.isInstance(t) => sender(t)
    case _ =>
  }
}