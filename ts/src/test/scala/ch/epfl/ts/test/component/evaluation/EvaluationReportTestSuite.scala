package ch.epfl.ts.test.component.evaluation

import ch.epfl.ts.evaluation.EvaluationReport
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import ch.epfl.ts.data.Currency._


@RunWith(classOf[JUnitRunner])
class EvaluationReportTestSuite extends FunSuite {
  val a = EvaluationReport(100L, "a", Map(), EUR, 10.0, 50.0, 2.4, 0.3, 20, 20)
  val b = EvaluationReport(101L, "b", Map(), EUR, 10.0, 50.0, 2.5, 0.3, 20, 10)

  test("compare report a < b") {
    assert(a < b)
  }

  test("compare report a > b") {
    assert(b > a)
  }

  test("compare report a = b") {
    val a = EvaluationReport(101L, "b", Map(), EUR, 10.0, 50.0, 2.4, 0.3, 20, 10)
    val b = EvaluationReport(100L, "a", Map(), EUR, 10.0, 50.0, 2.4, 0.3, 20, 10)
    assert(a >= b && a <= b)
  }

  test("compare with sorted") {
    val seq = Seq(a, b)
    assert(seq.sorted[EvaluationReport] == Seq(a, b))
  }

  test("compare with sortBy") {
    val seq = Seq(a, b)
   assert(seq.sortBy(_.sharpeRatio) == Seq(b, a))
  }

  test("compare with sortWith") {
    val seq = Seq(a, b)
    assert(seq.sortWith(_.sharpeRatio > _.sharpeRatio) == Seq(a, b))
  }
}

