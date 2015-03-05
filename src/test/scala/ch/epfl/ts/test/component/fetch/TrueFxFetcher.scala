package ch.epfl.ts.test.component.fetch

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import ch.epfl.ts.component.fetch.TrueFxFetcher


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
  
}