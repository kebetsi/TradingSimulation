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

/**
 * Type of orders : You send a bid order if you want to buy a security at a given price.
 *                  You send a ask order if you want to sell a security.
 *                
 * Limit order :    Limit order are left open during a certain period of time and are match if the currency
 *                  reach the given price.  
 * 
 * Market order :   Those orders are immediately executed at the current price 
 * 
 * Note that the price of a currency is the value of 1 unit of WhatC currency expressed in WithC currency
 * 
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 */
abstract class LimitOrder extends Order

/**
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */ 
case class LimitBidOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends LimitOrder

/**
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */ 
case class LimitAskOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends LimitOrder

abstract class MarketOrder extends Order

/**
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */ 
case class MarketBidOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends MarketOrder

/**
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */ 
case class MarketAskOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends MarketOrder

/**
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */ 
case class DelOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double)
  extends Order


/**
 * 
 * Represents an Open-High-Low-Close tick, with volume and timestamp (beginning of the tick)
 * 
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */
case class OHLC(marketId: Long, open: Double, high: Double, low: Double, close: Double, volume: Double, timestamp: Long, duration: Long) extends Streamable

/**
 * Forex-style data
 *
 *@param : WhatC represent which currency we are buying
 *@param : WithC represent the currency with which we are buying
 *
 * @see LimitOrder
 */
case class Quote(marketId: Long, timestamp: Long, whatC: Currency, withC: Currency, bid: Double, ask: Double) {
  override def toString() = "(" + whatC.toString().toUpperCase() + "/" + withC.toString().toUpperCase() + ") = (" + bid + ", " + ask + ")";
}


/**
 * Data Transfer Object representing a Tweet
 * @param timestamp
 * @param content
 * @param sentiment
 * @param imagesrc
 * @param author
 */
case class Tweet(timestamp: Long, content: String, sentiment: Int, imagesrc: String, author: String) extends Streamable
