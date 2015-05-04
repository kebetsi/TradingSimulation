package ch.epfl.ts.traders

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.data.CoefficientParameter
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.Order
import ch.epfl.ts.data.ParameterTrait
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.TimeParameter

/**
 * Required and optional parameters used by this strategy
 */
object MadTrader extends TraderCompanion {
  type ConcreteTrader = MadTrader
  override protected val concreteTraderTag = scala.reflect.classTag[MadTrader]
  
  /** Interval between two random trades (in ms) */
  val INTERVAL = "interval"

	/** Initial delay before the first random trade (in ms) */
  val INITIAL_DELAY = "initial_delay"

  /** Volume of currency to trade (in currency unit) */
  val ORDER_VOLUME = "order_volume"
  /** Random variations on the volume (in percentage of the order volume, both above and below `ORDER_VOLUME`) */
  val ORDER_VOLUME_VARIATION = "order_volume_variation"

  /** Which currencies to trade */
  val CURRENCY_PAIR = "currency_pair"

  override def strategyRequiredParameters: Map[Key, ParameterTrait] = Map(
      INTERVAL -> TimeParameter,
      ORDER_VOLUME -> NaturalNumberParameter,
      CURRENCY_PAIR -> CurrencyPairParameter
    )
  override def optionalParameters: Map[Key, ParameterTrait] = Map(
      INITIAL_DELAY -> TimeParameter,
      ORDER_VOLUME_VARIATION -> CoefficientParameter
    )
}

/**
 * Trader that gives just random ask and bid orders alternatively
 */
class MadTrader(uid: Long, parameters: StrategyParameters) extends Trader(uid, parameters) {
  import context._
  override def companion = MadTrader

  private case object SendMarketOrder

  // TODO: this initial order ID should be unique in the system
  var orderId = 4567

  val initialDelay = parameters.getOrDefault[FiniteDuration](MadTrader.INITIAL_DELAY, TimeParameter)
  val interval = parameters.get[FiniteDuration](MadTrader.INTERVAL)
  val volume = parameters.get[Int](MadTrader.ORDER_VOLUME)
  val volumeVariation = parameters.getOrElse[Double](MadTrader.ORDER_VOLUME_VARIATION, 0.1)
  val currencies = parameters.get[(Currency.Currency, Currency.Currency)](MadTrader.CURRENCY_PAIR)

  var alternate = 0
  val r = new Random

  // TODO: make wallet-aware
  override def receiver = {
    case SendMarketOrder => {
      // Randomize volume and price
      val variation = volumeVariation * (r.nextDouble() - 0.5) * 2.0
      val theVolume = ((1 + variation) * volume).toInt
      // Since we place a Market order, the price set here isn't used
      val dummyPrice = -1

      if (alternate % 2 == 0) {
        println("MadTrader: sending market bid order")
        send[Order](MarketAskOrder(orderId, uid, System.currentTimeMillis(), currencies._1, currencies._2, theVolume, dummyPrice))
      } else {
        println("MadTrader: sending market ask order")
        send[Order](MarketBidOrder(orderId, uid, System.currentTimeMillis(), currencies._1, currencies._2, theVolume, dummyPrice))
      }
      alternate = alternate + 1
      orderId = orderId + 1
    }
    case _ => println("MadTrader: received unknown")
  }

  /**
   * When simulation is started, plan ahead the next random trade
   */
  override def init = {
    system.scheduler.schedule(initialDelay, interval, self, SendMarketOrder)
  }
}
