package ch.epfl.ts.data

import Currency._

object OrderType extends Enumeration {
  type OrderType = Value
  val BID = Value("bid")
  val ASK = Value("ask")
};
import OrderType._

case class Order (price: Double, quantity: Double, timestamp: Long, currency: Currency, orderType: OrderType) {
  override def toString: String = { return "Order: price=" + price + ", quantity=" + quantity + ", timestamp=" + timestamp + ", currency=" + currency + ", orderType=" + orderType}
}


