package ch.epfl.ts.component.fetch

import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, Order}

import scala.io.Source

/**
 * load finance.csv (provided by Milos Nikolic) in OrderPersistor
 */
object CSVFetcher {
  def main(args: Array[String]) {
    val fetcher = new CSVFetcher
    fetcher.loadInPersistor("finance.csv")
  }
}

class CSVFetcher {

  def loadInPersistor(filename: String) {
    // name the db as "[filename without extension].db"
    val ordersPersistor = new OrderPersistor(filename.replaceAll("\\.[^.]*$", ""))
    ordersPersistor.init()
    var counter = 0
    val startTime = System.currentTimeMillis()
    val source = Source.fromFile(filename)
    var line: Array[String] = new Array[String](5)

    println("Start reading in.")

    var orders = List[Order]()
    source.getLines().foreach {
      s => {
        if (counter % 1000 == 0) {
          ordersPersistor.save(orders)
          println(System.currentTimeMillis() - startTime + "\t" + counter)
          orders = List[Order]()
        }

        counter += 1
        line = s.split(",")
        line(2) match {
          case "B" => orders = LimitBidOrder(line(1).toLong, 0, line(0).toLong, USD, USD, line(3).toDouble, line(4).toDouble) :: orders
          case "S" => orders = LimitAskOrder(line(1).toLong, 0, line(0).toLong, USD, USD, line(3).toDouble, line(4).toDouble) :: orders
          case "D" => orders = DelOrder(line(1).toLong, 0, line(0).toLong, DEF, DEF, 0, 0) :: orders
          case _ =>
        }
      }
    }

    println("Done!")
  }
}