package ch.epfl.ts.data

import Currency._

object OrderType extends Enumeration {
  type OrderType = Value
  val BID = Value("B")
  val ASK = Value("S")
  val DEL = Value("D")
}
import OrderType._

case class Order (id: Long, price: Double, quantity: Double, timestamp: Long, currency: Currency, orderType: OrderType)
  extends StreamObject {
  override def toString: String = "Order: timestamp=" + timestamp + ", id=" + id + ", orderType=" + orderType + ", volume=" + timestamp + ", volume=" + quantity + ", price=" + price + ", currency=" + currency
}


