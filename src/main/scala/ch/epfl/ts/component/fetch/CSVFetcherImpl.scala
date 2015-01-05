package ch.epfl.ts.component.fetch

import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{ Order, OrderType, LimitBidOrder, LimitAskOrder, DelOrder }
import scala.io.Source

object CSVFetcherImpl {
  def main(args: Array[String]) {
    val fetcher = new CSVFetcherImpl
    fetcher.loadInPersistor("finance.csv")
  }
}

class CSVFetcherImpl {

  def loadInPersistor(filename: String) {
    // name the db as "[filename without extension].db"
    val ordersPersistor = new OrderPersistor(filename.replaceAll("\\.[^.]*$", ".db"))
    ordersPersistor.init()

    val source = Source.fromFile(filename)
    val iter = source.getLines().toList
    var line: Array[String] = new Array[String](5)

    iter.map { x =>
      line = x.split(",")
      if (!(line(2).equals("C") || line(2).equals("E") || line(2).equals("T") || line(2).equals("X"))) {
        line(2) match {
          case "B" => ordersPersistor.save(LimitBidOrder(line(1).toLong, 0, line(0).toLong, USD, USD, line(3).toDouble, line(4).toDouble))
          case "S" => ordersPersistor.save(LimitAskOrder(line(1).toLong, 0, line(0).toLong, USD, USD, line(3).toDouble, line(4).toDouble))
          case "D" => ordersPersistor.save(DelOrder(line(1).toLong, 0, line(0).toLong, DEF, DEF, 0, 0))
        }
      }
    }
  }
}