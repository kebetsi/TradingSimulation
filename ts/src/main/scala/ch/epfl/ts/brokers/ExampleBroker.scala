package ch.epfl.ts.brokers

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Register
import akka.actor.ActorRef

/**
 * Created by sygi on 03.04.15.
 */
class ExampleBroker extends Component {
  var mapping = Map[Long, ActorRef]()
  override def receiver: PartialFunction[Any, Unit] = {
    case Register(id) => {
      println("Broker: registration of agent " + id)
      println("with ref: " + sender())
      mapping = mapping + (id -> sender())
    }
    case p => println("Broker: received unknown " + p)
  }
}
