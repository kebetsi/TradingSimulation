import ch.epfl.ts.data.Order
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.impl.OrderPersistorImpl
import ch.epfl.ts.types.Currency._

object Test {

  def main(args: Array[String]) {
    val order1 = new Order(2.0, 3.0, 1, USD, ASK)
    val order2 = new Order(1.0, 2.0, 2, USD, ASK)
    val persistor = new OrderPersistorImpl
    persistor.init()
    persistor.save(order1)
    persistor.save(order2)
//    val retrievedOrder = persistor.loadSingle(1)
//    println("retrieved: price=" + retrievedOrder.price + ", quantity=" + retrievedOrder.quantity + ", timestamp=" + retrievedOrder.timestamp + ", currency=" + retrievedOrder.currency + ", orderType=" + retrievedOrder.orderType)
    val orders = persistor.loadBatch(0, 2)
    orders.map(a => println(a))
    persistor.deleteBatch(0, 100)
  }

}