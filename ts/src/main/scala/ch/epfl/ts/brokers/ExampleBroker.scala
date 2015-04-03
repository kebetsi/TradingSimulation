package ch.epfl.ts.brokers

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Register

/**
 * Created by sygi on 03.04.15.
 */
class ExampleBroker extends Component {
  override def receiver: PartialFunction[Any, Unit] = {
    case Register(id) => println("Broker: registration of agent " + id)
    case p => println("Broker: received unknown " + p)
  }
}
