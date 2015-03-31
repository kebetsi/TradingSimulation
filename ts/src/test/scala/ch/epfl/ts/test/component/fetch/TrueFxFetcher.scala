package ch.epfl.ts.test.component.fetch

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.data.Quote
import scala.tools.nsc.interpreter.Power


@RunWith(classOf[JUnitRunner]) 
class TrueFxFetcherTestSuite extends FunSuite {
  val fetcher: TrueFxFetcher = new TrueFxFetcher()
  val nRequests = 10
  val delay = 500L // Wait for 1 second between each request
  
  test("TrueFX API can be reached") {
    val fetched = fetcher.fetch()
    assert(fetched.length > 0)
  }
  
  test("TrueFX API allows several consecutive requests") {
    for(i <- 1 to nRequests) {
    	val fetched = fetcher.fetch()
    	assert(fetched.length > 0)   
      Thread.sleep(delay)
    }
  }
 
  test("Should give out all significant digits") {
    val fetched = fetcher.fetch()
    val significantDigits = 6

    def extractStrings(quotes: List[Quote]): List[String] = fetched.flatMap(q => q match {
      case Quote(_, _, _, _, bid, ask) => List(bid.toString(), ask.toString())
    })
    
    /**
     * To verify we are given enough digits, check the last digit
     * is different for the various prices.
     * Although it may report a false negative with extremely low
     * probability, it will always fail if less significant digits
     * are given.
     * Warning Prices are on different scales, we should take care that:
     *   e.g. JPY 128.634  has as many significant digits as CHF 1.06034
     */
    val strings = extractStrings(fetched)
    
    // +1 takes into account the '.' separator, +2 is too much
    assert(strings.exists { s => s.length() == significantDigits + 1 })
    assert(strings.forall { s => s.length() < significantDigits + 2 })
  }
}