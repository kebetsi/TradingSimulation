package ch.epfl.ts.component.fetch

import ch.epfl.ts.component.persist.OrderPersistor
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{Order, OrderType}

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
        ordersPersistor.save(new Order(line(1).toLong, line(4).toDouble, line(3).toDouble, line(0).toLong, USD, OrderType.withName(line(2))))
      }
    }
  }
}