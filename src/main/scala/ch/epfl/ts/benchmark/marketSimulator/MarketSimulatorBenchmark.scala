package ch.epfl.ts.benchmark.marketSimulator

import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.data.{ Order, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, DelOrder }
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.component.ComponentBuilder
import akka.actor.Props
import ch.epfl.ts.engine.{ MarketSimulator, MarketRules }

/**
 * file containing various tests to benchmark the MarketSimulator's performance
 */
object MarketSimulatorBenchmark {

  var r = scala.util.Random

  def main(args: Array[String]) {
      throughputBenchmark

  }

  /**
   * compute the processing performance of the Market Simulator. It computes the time needed to process a certain
   * amount of orders (either generated or loaded from finance.csv). The displayed time is the difference between
   * the sending of the first order until the processing of the last order.
   */
  def throughputBenchmark = {
        val orders = loadOrdersFromPersistor(100000, "finance")
//    val orders = generateOrders(150000)

    // create factory
    implicit val builder = new ComponentBuilder("MarketSimulatorBenchmarkSystem")

    // Create Components
    val orderFeeder = builder.createRef(Props(classOf[OrderFeeder], orders))
    val market = builder.createRef(Props(classOf[BenchmarkMarketSimulator], 1L, new BenchmarkMarketRules()))
    val timeCounter = builder.createRef(Props(classOf[TimeCounter]))

    // Create Connections
    //orders
    orderFeeder.addDestination(market, classOf[LimitAskOrder])
    orderFeeder.addDestination(market, classOf[LimitBidOrder])
    orderFeeder.addDestination(market, classOf[MarketAskOrder])
    orderFeeder.addDestination(market, classOf[MarketBidOrder])
    orderFeeder.addDestination(market, classOf[DelOrder])
    orderFeeder.addDestination(market, classOf[LastOrder])
    // start and end signals
    orderFeeder.addDestination(timeCounter, classOf[StartSending])
    market.addDestination(timeCounter, classOf[FinishedProcessingOrders])

    // start the benchmark
    builder.start
  }

  def loadOrdersFromPersistor(count: Int, persistorName: String): List[Order] = {
    val financePersistor = new OrderPersistor(persistorName) // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    var orders: List[Order] = Nil // 34105074
    orders = financePersistor.loadBatch(25210389, 35724618)
    //    for(i <- 1 to count) {
    //      if (i % 100 == 0) println("loaded " + i + "th order from persistor")
    //      orders = financePersistor.loadSingle(i) :: orders
    //    }
    println("loaded " + orders.size + " orders")
    orders
  }

  def generateOrders(count: Int): List[Order] = {
    val initTime = System.currentTimeMillis()
    // first generate a list of orders to feed the market simulator
    // we'll take the percentages from finance.csv and make 10% of LA and LB orders MA and MB orders respectively
    // since we have removed 10% of limit orders, we will also remove 10% of delete orders and add them
    // to MA and MB orders
    // so: LB = 23.4%, LA = 35.1%, MB = 4.2%, MA = 5.7%, DEL = 31.6%
    // implementation: LB=0-233, LA=234-584, MB=585-626, MA=627-683, DEL=684-999
    var orders: List[Order] = Nil
    var oid: Int = 0
    // store used oids
    var lbOids: Set[Int] = Set[Int]()
    var laOids: Set[Int] = Set[Int]()
    // set trading price params
    val tradingPrice = 100
    val spread = 20

    // generate relevant prices
    for (i <- 1 to count) {
      if (i % 10000 == 0) {
        println("generating " + i + "th order.")
      }
      r.nextInt(1000) match {
        // Limit Bid Order
        case it if 0 until 234 contains it => {
          oid = oid + 1
          lbOids += oid
          orders = new LimitBidOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), generatePrice(tradingPrice, spread)) :: orders
        }
        // Limit Ask Order
        case it if 234 until 585 contains it => {
          oid = oid + 1
          laOids += oid
          orders = new LimitAskOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), generatePrice(tradingPrice, spread)) :: orders
        }
        // Market Bid Order
        case it if 585 until 627 contains it => {
          oid = oid + 1
          orders = new MarketBidOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), 0.0) :: orders
        }
        // Market Ask Order
        case it if 627 until 684 contains it => {
          oid = oid + 1
          orders = new MarketAskOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), 0.0) :: orders
        }
        // Del Order
        case it if 684 until 1000 contains it => {
          r.nextInt(585) match {
            // generate delete order for limit bid order
            case it2 if 0 until 234 contains it2 => {
              if (lbOids.size > 0) {
                val lbIdToDelete = lbOids.toVector(r.nextInt(lbOids.size))
                orders = new DelOrder(lbIdToDelete, 0L, System.currentTimeMillis(), BTC, USD, 0.0, 0.0) :: orders
                lbOids -= lbIdToDelete
              }
            }
            // generate delete order for limit ask order
            case it2 if 234 until 585 contains it2 => {
              if (laOids.size > 0) {
                val laIdToDelete = laOids.toVector(r.nextInt(laOids.size))
                orders = new DelOrder(laIdToDelete, 0L, System.currentTimeMillis(), BTC, USD, 0.0, 0.0) :: orders
                laOids -= laIdToDelete
              }
            }
          }
        }
      }
    }
    println("generated " + orders.size + " orders in " + (System.currentTimeMillis() - initTime) + " ms.")
    countOrderTypes(orders)
    orders
  }

  // generates a price value in the range: [tradingPrice - spread/2; tradingPrice + spread/2]
  def generatePrice(tradingPrice: Double, spread: Double): Double = {
    tradingPrice - (spread / 2) + r.nextDouble() * spread
  }

  // generates a value between 10 and rangeTimesTen * 10
  def generateOrderVolume(rangeTimesTen: Int): Int = {
    (r.nextInt(rangeTimesTen) + 1) * 10
  }

  // print the generated orders distribution
  def countOrderTypes(orders: List[Order]) = {
    var laCount = 0
    var lbCount = 0
    var maCount = 0
    var mbCount = 0
    var delCount = 0
    orders.map { x =>
      x match {
        case o: LimitBidOrder  => lbCount = lbCount + 1
        case o: LimitAskOrder  => laCount = laCount + 1
        case o: MarketAskOrder => maCount = maCount + 1
        case o: MarketBidOrder => mbCount = mbCount + 1
        case o: DelOrder       => delCount = delCount + 1
      }
    }
    println("Total: " + orders.size + ", LA: " + laCount + ", LB: " + lbCount + ", MA: " + maCount + ", MB: " + mbCount + ", DEL: " + delCount)
  }

  // count the occurences of different types of orders in finance.csv to get an idea of how to generate fake orders for the benchmarking
  // results: LB orders= 26%, LA orders = 39%, DEL orders = 35%
  def countOrderDistributionInFinanceCSV = {
    val financePersistor = new OrderPersistor("finance") // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    var laOrders: Int = 0
    var lbOrders: Int = 0
    var delOrders: Int = 0
    var order: Order = null
    for (i <- 1 to 50000) {
      financePersistor.loadSingle(i) match {
        case lb: LimitBidOrder => laOrders = laOrders + 1
        case la: LimitAskOrder => lbOrders = lbOrders + 1
        case del: DelOrder     => delOrders = delOrders + 1
        case _                 =>
      }
    }
    println("LB orders: " + lbOrders + ", LA orders: " + laOrders + ", DEL orders: " + delOrders)
  }
}