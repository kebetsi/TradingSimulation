package ch.epfl.ts.data

import Currency._

case class Transaction(price: Double, quantity: Double, timestamp: Long, currency: Currency, buyer: String, seller: String) {
  override def toString: String = {"Transaction: price=" + price + ", quantity=" + quantity + ", timestamp=" + timestamp + ", currency=" + currency + ", buyer=" + buyer + ", seller=" + seller}
}