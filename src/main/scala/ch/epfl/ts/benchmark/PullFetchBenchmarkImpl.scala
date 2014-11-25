package ch.epfl.ts.benchmark

import scala.io.Source
import ch.epfl.ts.first.PullFetch

class PullFetchBenchmarkImpl extends PullFetch[String] {
override def interval = 12000

  val source = Source.fromURL("/BITCOIN-BITSTAMPUSD.csv")

  override def fetch: List[String] = {
    return source.getLines().toList
  }
}