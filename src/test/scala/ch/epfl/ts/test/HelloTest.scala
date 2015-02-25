package ch.epfl.ts.test

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

@RunWith(classOf[JUnitRunner]) 
class HelloTest extends FunSuite {
  
  test("My first test") {
    assert(2 + 2 == 4)
  }  
  
}