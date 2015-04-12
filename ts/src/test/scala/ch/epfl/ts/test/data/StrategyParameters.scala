package ch.epfl.ts.test.data

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import ch.epfl.ts.data.Currency
import scala.util.Try
import ch.epfl.ts.data.CurrencyPairParameter

@RunWith(classOf[JUnitRunner])
class StrategyParametersTests extends FunSuite {
  
  test("Should allow to add several parameters at a time") {
    assert(false, "Test not implemented")
  }

  test("Should accept valid values for a parameter") {
	  assert(false, "Test not implemented")
  }
  
  test("Should reject invalid values for a parameter") {
    assert(false, "Test not implemented")
  }
  
  test("Should hold the parameters and yield back their values") {
	  assert(false, "Test not implemented")
  }
}

@RunWith(classOf[JUnitRunner])
class CurrencyPairParameterTests extends FunSuite {
  test("Should give back the value it was instantiated with") {
    val myPair = (Currency.EUR, Currency.CHF)
    val parameter = CurrencyPairParameter(myPair)
    assert(parameter.get() == myPair)
  }
  
  test("Should reject a pair of twice the same currency") {
    val attempt = Try(CurrencyPairParameter((Currency.EUR, Currency.EUR)))
    assert(attempt.isFailure)
  }

  test("Should accept all values within the acceptable range") {
	  for {
      value <- CurrencyPairParameter.validValues
      attempt = Try(CurrencyPairParameter(value))
    } yield assert(attempt.isSuccess, "Should accept " + value)
  }
}