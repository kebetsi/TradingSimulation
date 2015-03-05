package ch.epfl.ts.data

/**
 * Enum for Currencies
 */
object Currency extends Enumeration {
  type Currency = Value
  // Cryptocurrencies
  val BTC = Value("btc")
  val LTC = Value("ltc")
  
  // Real-life currencies
  val USD = Value("usd")
  val CHF = Value("chf")
  val RUR = Value("rur")
  val EUR = Value("eur")
  val JPY = Value("jpy")
  val GBP = Value("gbp")
  val AUD = Value("aud")
  
  // Fallback ("default")
  val DEF = Value("def")
}

import ch.epfl.ts.data.Currency._


/**
 * Definition of the System's internal messages.
 */
trait Streamable

/**
 * Data Transfer Object representing a Transaction
 * @param mid market id
 * @param price 
 * @param volume
 * @param timestamp
 * @param whatC
 * @param withC
 * @param buyerId buyer user id
 * @param buyOrderId buyer order id
 * @param sellerId seller user id
 * @param sellOrderId seller order id
 */
case class Transaction(mid: Long, price: Double, volume: Double, timestamp: Long, whatC: Currency, withC: Currency, buyerId: Long, buyOrderId: Long, sellerId: Long, sellOrderId: Long) extends Streamable

trait AskOrder
trait BidOrder

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

abstract class LimitOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends Order(oid, uid, timestamp, whatC, withC, volume, price)

case class LimitBidOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends LimitOrder(oid, uid, timestamp, whatC, withC, volume, price) with BidOrder

case class LimitAskOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends LimitOrder(oid, uid, timestamp, whatC, withC, volume, price) with AskOrder

abstract class MarketOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends Order(oid, uid, timestamp, whatC, withC, volume, price)

case class MarketBidOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends MarketOrder(oid, uid, timestamp, whatC, withC, volume, price) with BidOrder

case class MarketAskOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends MarketOrder(oid, uid, timestamp, whatC, withC, volume, price) with AskOrder

case class DelOrder(override val oid: Long, override val uid: Long, override val timestamp: Long, override val whatC: Currency, override val withC: Currency, override val volume: Double, override val price: Double)
  extends Order(oid, uid, timestamp, whatC, withC, volume, price)


/**
 * represents an Open-High-Low-Close tick, with volume and timestamp (beginning of the tick)
 */
case class OHLC(marketId: Long, open: Double, high: Double, low: Double, close: Double, volume: Double, timestamp: Long, duration: Long) extends Streamable


/**
 * Data Transfer Object representing a Tweet
 * @param timestamp
 * @param content
 * @param sentiment
 * @param imagesrc
 * @param author
 */
case class Tweet(timestamp: Long, content: String, sentiment: Int, imagesrc: String, author: String) extends Streamable
