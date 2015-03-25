package ch.epfl.ts.data

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
abstract class Order() extends Streamable {
  def oid: Long
  def uid: Long
  def timestamp: Long
  def whatC: Currency
  def withC: Currency
  def volume: Double
  def price: Double
}

abstract class LimitOrder extends Order

case class LimitBidOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends LimitOrder

case class LimitAskOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends LimitOrder

abstract class MarketOrder extends Order

case class MarketBidOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends MarketOrder

case class MarketAskOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends MarketOrder

case class DelOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends Order


/**
 * Represents an Open-High-Low-Close tick, with volume and timestamp (beginning of the tick)
 */
case class OHLC(marketId: Long, open: Double, high: Double, low: Double, close: Double, volume: Double, timestamp: Long, duration: Long) extends Streamable

/**
 * Forex-style data
 * @TODO: we also have access to 'bid points' and 'offer points'. Do we need those?
 */
case class Quote(marketId: Long, timestamp: Long, whatC: Currency, withC: Currency, bid: Double, ask: Double)


/**
 * Data Transfer Object representing a Tweet
 * @param timestamp
 * @param content
 * @param sentiment
 * @param imagesrc
 * @param author
 */
case class Tweet(timestamp: Long, content: String, sentiment: Int, imagesrc: String, author: String) extends Streamable
