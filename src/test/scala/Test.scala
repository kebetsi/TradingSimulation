import ch.epfl.ts.data.Order
import ch.epfl.ts.impl.OrderPersistorImpl

object Test {

  def main(args: Array[String]) {
    val order = new Order(2.0, 3.0, 1)
    val persistor = new OrderPersistorImpl
    persistor.init()
    persistor.save(order)
    val retrievedOrder = persistor.loadSingle(10)
    println("retrieved: price=" + retrievedOrder.price + ", quantity=" + retrievedOrder.quantity + ", timestamp=" + retrievedOrder.timestamp)
  }

}