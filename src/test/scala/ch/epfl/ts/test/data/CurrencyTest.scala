package ch.epfl.ts.test.data

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import ch.epfl.ts.data.Currency


@RunWith(classOf[JUnitRunner]) 
class CurrencyTestSuite extends FunSuite {
  
  test("toString and fromString are inverting each other") {
    val currencies = Currency.values;
    assert(currencies.forall { c => Currency.fromString(c.toString) == c })
  }
}