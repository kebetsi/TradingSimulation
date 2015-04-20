package ch.epfl.ts.traders

import scala.util.Random
import scala.concurrent.duration.FiniteDuration
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.Order
import ch.epfl.ts.component.StartSignal
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.data.ParameterTrait
import ch.epfl.ts.data.CurrencyPairParameter
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.CoefficientParameter
import ch.epfl.ts.data.NaturalNumberParameter
import ch.epfl.ts.data.TimeParameter
import ch.epfl.ts.component.ComponentBuilder

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
  /** Random variations on the volume (in percentage of the order volume) */
  val ORDER_VOLUME_VARIATION = "order_volume_variation"

  /** Which currencies to trade */
  val CURRENCY_PAIR = "currency_pair"

  override def requiredParameters: Map[Key, ParameterTrait] = Map(
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
class MadTrader(uid: Long, parameters: StrategyParameters) extends Trader(parameters) {
  import context._
  private case object SendMarketOrder

  def companion = MadTrader

  // TODO: this initial order Id should be unique in the system
  var orderId = 4567

  val initialDelay: FiniteDuration = parameters.getOrDefault(MadTrader.INITIAL_DELAY, TimeParameter)
  val interval: FiniteDuration = parameters.get(MadTrader.INTERVAL)
  val volume: Int = parameters.get(MadTrader.ORDER_VOLUME)
  val volumeVariation: Double = parameters.get(MadTrader.ORDER_VOLUME_VARIATION)
  val currencies: (Currency.Currency, Currency.Currency) = parameters.get(MadTrader.INTERVAL)

  var alternate = 0
  val r = new Random

  override def receiver = {
    case StartSignal => start
    case SendMarketOrder => {
      // Randomize volume and price
      val theVolume = ((1 + volumeVariation * r.nextDouble()) * volume).toInt
      // Since we place a Market order, the price set here isn't used
      val dummyPrice = -1

      if (alternate % 2 == 0) {
        println("SimpleTrader: sending market bid order")
        send[Order](MarketAskOrder(orderId, uid, System.currentTimeMillis(), currencies._1, currencies._2, theVolume, dummyPrice))
      } else {
        println("SimpleTrader: sending market ask order")
        send[Order](MarketBidOrder(orderId, uid, System.currentTimeMillis(), currencies._1, currencies._2, theVolume, dummyPrice))
      }
      alternate = alternate + 1
      orderId = orderId + 1
    }
    case _ => println("SimpleTrader: received unknown")
  }

  /**
   * When simulation is started, plan ahead the next random trade
   */
  override def start = {
    system.scheduler.schedule(initialDelay, interval, self, SendMarketOrder)
  }
}
