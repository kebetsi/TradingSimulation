package ch.epfl.ts.data

import ch.epfl.ts.types.Currency._

object OrderType extends Enumeration {
  type OrderType = Value
  val BID = Value("bid")
  val ASK = Value("ask")
};
import OrderType._

case class Order (price: Double, quantity: Double, timestamp: Long, currency: Currency, orderType: OrderType) {
}


