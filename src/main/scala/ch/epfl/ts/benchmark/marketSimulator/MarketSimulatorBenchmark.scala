package ch.epfl.ts.benchmark.marketSimulator

import scala.collection.mutable.ListBuffer
import scala.io.StdIn

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.data.Currency.BTC
import ch.epfl.ts.data.Currency.USD
import ch.epfl.ts.data.DelOrder
import ch.epfl.ts.data.LimitAskOrder
import ch.epfl.ts.data.LimitBidOrder
import ch.epfl.ts.data.MarketAskOrder
import ch.epfl.ts.data.MarketBidOrder
import ch.epfl.ts.data.Order

/**
 * This performance test calculates the time it takes for the MarketSimulator 
 * to process a certain amount of orders.
 * It is possible to use a Persistor as source or to generate orders for
 * input data.
 * To use data stored in a Persistor, use the loadOrdersFromPersistor()
 * function to fill the orders variable.
 * To use generated orders, use the generateOrders() method.
 */
object MarketSimulatorBenchmark {

  var r = scala.util.Random

  def main(args: Array[String]) {

    //val orders = loadOrdersFromPersistor(500000, "finance")
    val orders = generateOrders(250000)
    
    println(orders.length)
    
    // create factory
    implicit val builder = new ComponentBuilder("MarketSimulatorBenchmarkSystem")

    // Create Components
    val orderFeeder = builder.createRef(Props(classOf[OrderFeeder], orders), "feeder")
    val market = builder.createRef(Props(classOf[BenchmarkOrderBookMarketSimulator], 1L, new BenchmarkMarketRules()), "marketSim")
    val timeCounter = builder.createRef(Props(classOf[TimeCounter]), "timeCounter")

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
    StdIn.readLine("Press ENTER to exit...")
    builder.system.shutdown()
    builder.system.awaitTermination()
  }

  def generateOrders(count: Int): List[Order] = {
    val initTime = System.currentTimeMillis()
    // first generate a list of orders to feed the market simulator
    // we'll take the percentages from finance.csv and make 10% of LA and LB orders MA and MB orders respectively
    // since we have removed 10% of limit orders, we will also remove 10% of delete orders and add them
    // to MA and MB orders
    // so: LB = 23.4%, LA = 35.1%, MB = 4.2%, MA = 5.7%, DEL = 31.6%
    // implementation: LB=0-233, LA=234-584, MB=585-626, MA=627-683, DEL=684-999
    var orders: ListBuffer[Order] = ListBuffer[Order]()
    var oid: Int = 0
    // store used order ids
    var lbOids: Set[Int] = Set[Int]()
    var laOids: Set[Int] = Set[Int]()
    // set trading price params
    val tradingPrice = 100
    val spread = 20
    
    // generate relevant prices
    while (orders.length <= count) {
      if (orders.length % 10000 == 0) {
        println("generating " + orders.length + "th order.")
      }

      val it = r.nextInt(1000)
      // Limit Bid Order
      if ((it >= 0) && (it < 234)) {
        oid = oid + 1
        lbOids += oid
        orders.append(LimitBidOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), generatePrice(tradingPrice, spread)))
      }
      // Limit Ask Order
      else if ((it >= 243) && (it < 585)) {
        oid = oid + 1
        laOids += oid
        orders.append(LimitAskOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), generatePrice(tradingPrice, spread)))
      }
      // Market Bid Order
      else if ((it >= 585) && (it < 627)) {
        oid = oid + 1
        orders.append(MarketBidOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), 0.0))
      }
      // Market Ask Order
      else if ((it >= 627) && (it < 684)) {
        oid = oid + 1
        orders.append(MarketAskOrder(oid, 0L, System.currentTimeMillis(), BTC, USD, generateOrderVolume(10), 0.0))
      }
      // Del Order
      else if ((it >= 684) && (it <= 1000)) {
        val it2 = r.nextInt(585)
        // generate delete order for limit bid order
        if ((it2 >= 0) && (it2 < 234)) {
          if (lbOids.size > 0) {
            val lbIdToDelete = lbOids.toVector(r.nextInt(lbOids.size))
            orders.append(DelOrder(lbIdToDelete, 0L, System.currentTimeMillis(), BTC, USD, 0.0, 0.0))
            lbOids -= lbIdToDelete
          }
        }
        // generate delete order for limit ask order
        else if ((it2 >= 234) && (it2 <= 585)) {
          if (laOids.size > 0) {
            val laIdToDelete = laOids.toVector(r.nextInt(laOids.size))
            orders.append(DelOrder(laIdToDelete, 0L, System.currentTimeMillis(), BTC, USD, 0.0, 0.0))
            laOids -= laIdToDelete
          }
        }
      }
    }
    println("generated " + orders.size + " orders in " + (System.currentTimeMillis() - initTime) + " ms.")
    countOrderTypes(orders.toList)
    orders.toList
  }

  // generates a price value in the range: [tradingPrice - spread/2; tradingPrice + spread/2]
  def generatePrice(tradingPrice: Double, spread: Double): Double = {
    tradingPrice - (spread / 2) + r.nextDouble() * spread
  }

  // generates a value between 10 and rangeTimesTen * 10
  def generateOrderVolume(rangeTimesTen: Int): Int = {
    (r.nextInt(rangeTimesTen) + 1) * 10
  }

  def loadOrdersFromPersistor(count: Int, persistorName: String): List[Order] = {
    val financePersistor = new OrderPersistor(persistorName) // requires to have run CSVFetcher on finance.csv (obtained by mail from Milos)
    financePersistor.init()
    var orders: List[Order] = Nil
    orders = financePersistor.loadBatch(count)

//    for(i <- 1 to count) {
//      if (i % 100 == 0) println("loaded " + i + "th order from persistor")
//      orders = financePersistor.loadSingle(i) :: orders
//    }
    orders
  }

  // print the generated orders distribution
  def countOrderTypes(orders: List[Order]) = {
    var laCount = 0
    var lbCount = 0
    var maCount = 0
    var mbCount = 0
    var delCount = 0
    orders.map {
        case o: LimitBidOrder => lbCount = lbCount + 1
        case o: LimitAskOrder => laCount = laCount + 1
        case o: MarketAskOrder => maCount = maCount + 1
        case o: MarketBidOrder => mbCount = mbCount + 1
        case o: DelOrder => delCount = delCount + 1
    }
    println("Total: " + orders.size + ", LA: " + laCount + ", LB: " + lbCount + ", MA: " + maCount + ", MB: " + mbCount + ", DEL: " + delCount)
  }

  // count the occurences of different types of orders in finance.csv to get an idea of how to generate fake orders for the benchmarking
  // results: LB orders= 26%, LA orders = 39%, DEL orders = 35%
  def countOrderDistributionInFinanceCSV(): Unit = {
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
        case del: DelOrder => delOrders = delOrders + 1
        case _ =>
      }
    }
    println("LB orders: " + lbOrders + ", LA orders: " + laOrders + ", DEL orders: " + delOrders)
  }
}