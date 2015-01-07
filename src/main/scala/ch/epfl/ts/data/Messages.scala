package ch.epfl.ts.data

/**
 * Enum for Currencies
 */
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


/**
 * Definition of the System's internal messages.
 */
trait Streamable

/**
 * Data Transfer Object representing a Transaction
 * @param price
 * @param volume
 * @param timestamp
 * @param whatC
 * @param withC
 * @param buyerId
 * @param buyOrderId
 * @param sellerId
 * @param sellOrderId
 */
case class Transaction(price: Double, volume: Double, timestamp: Long, whatC: Currency, withC: Currency, buyerId: Long, buyOrderId: Long, sellerId: Long, sellOrderId: Long) extends Streamable

/**
 * Data Transfer Object representing a Order
 * @param oid
 * @param uid
 * @param timestamp
 * @param whatC
 * @param withC
 * @param volume
 * @param price
 */
abstract class Order(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double) extends Streamable

case class LimitOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)

case class LimitBidOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends LimitOrder(oid, uid, timestamp, whatC, withC, volume, price)

case class LimitAskOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends LimitOrder(oid, uid, timestamp, whatC, withC, volume, price)

case class MarketOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)

case class MarketBidOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends MarketOrder(oid, uid, timestamp, whatC, withC, volume, price)

case class MarketAskOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends MarketOrder(oid, uid, timestamp, whatC, withC, volume, price)

case class DelOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double) extends Order(oid, uid, timestamp, whatC, withC, volume, price)


/**
 * Data Transfer Object representing a Tweet
 * @param timestamp
 * @param content
 * @param sentiment
 * @param imagesrc
 * @param author
 */
case class Tweet(timestamp: Long, content: String, sentiment: Int, imagesrc: String, author: String) extends Streamable
