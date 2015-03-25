package ch.epfl.ts.benchmark.scala

import ch.epfl.ts.component.fetch.PullFetch
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Transaction

import scala.io.Source

class PullFetchBenchmarkImpl extends PullFetch[Transaction] {
  override def interval = 12000 * 1000 * 1000

  val filename = "fakeData.csv"
  var called = false

  override def fetch: List[Transaction] = {
    if (called) {
      List[Transaction]()
    } else {
      called = true
      val source = Source.fromFile(filename)
      val lines = source.getLines().toList
//      lines.map(_.split(",")).map(
//        l => Transaction(l(1).toDouble, l(2).toDouble, l(0).toLong, Currency.withName(l(3).toLowerCase), l(4), l(5))
//      )

      new Transaction(0, 1.0,1.0,1,USD, BTC, 1, 1, 1, 1) :: Nil
    }
  }
}