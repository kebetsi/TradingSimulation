//package ch.epfl.ts.data
//
//import Currency._
//
//case class Transaction(price: Double, quantity: Double, timestamp: Long, currency: Currency, buyerId: Long, buyOrderId: Long, sellerId: Long, sellOrderId: Long)
//  extends StreamObject {
//  override def toString: String = "Transaction: price=" + price + ", quantity=" + quantity + ", timestamp=" + timestamp + ", currency=" + currency + ", buyerId=" + buyerId + "buyOrderId=" + buyOrderId + ", sellerId=" + sellerId + ", sellOrderId=" + sellOrderId
//}