package ch.epfl.ts.data

object Currency extends Enumeration {
  type Currency = Value
  val BTC = Value("btc")
  val LTC = Value("ltc")
  val USD = Value("usd")
  val CHF = Value("chf")
  val RUR = Value("rur")
  val DEF = Value("def") // default
}
import ch.epfl.ts.data.Currency._

object OrderType extends Enumeration {
  type OrderType = Value
  val LIMIT_BID = Value("LB")
  val LIMIT_ASK = Value("LA")
  val MARKET_BID = Value("MB")
  val MARKET_ASK = Value("MA")
  val DEL = Value("D")
}

/*
 * Definition of the System's internal messages.
 */

trait Streamable

/* *****************************
 * Transactions
 */
case class Transaction(price: Double, volume: Double, timestamp: Long, whatC: Currency, withC: Currency, buyerId: Long, buyOrderId: Long, sellerId: Long, sellOrderId: Long) extends Streamable

/* *****************************
 * Orders
 */

/**
 * uid: user id
 * oid: order id
 */
abstract class Order(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double) extends Streamable
case class LimitBidOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)
case class LimitAskOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)
case class MarketBidOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)
case class MarketAskOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)
case class DelOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)
