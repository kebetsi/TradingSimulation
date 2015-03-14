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
    val significantDigits = 5
    
    def getDigit(x:Double, digits: Int): Int = ( (x * Math.pow(10, digits)) % 10 ).toInt
    def extractDigits(digits: Int) = fetched.flatMap(q => q match {
      case Quote(_, _, _, _, bid, ask) => List(getDigit(bid, digits), getDigit(ask, digits))
    })
    
    /**
     * To verify we are given enough digits, check the last digit
     * is different for the various prices.
     * Although it may report a false negative with extremely low
     * probability, it will always fail if less significant digits
     * are given.
     */
    val lastDigits = extractDigits(significantDigits)
    val tooManyDigits = extractDigits(significantDigits + 1)
    
    assert(lastDigits.exists { x => x != 0 })
    assert(tooManyDigits.forall { x => x == 0 })
  }
}