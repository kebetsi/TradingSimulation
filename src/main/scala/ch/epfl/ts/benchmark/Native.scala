package ch.epfl.ts.benchmark

import java.io._

import ch.epfl.ts.data.{Currency, Transaction}

import scala.io.Source
import scala.util.Random

object Native {

  def main(args: Array[String]) = {
    val filename = "fakeData.csv"
    val source = Source.fromFile(filename)
    println("Unbuffered run")
    val entriesList = timed(source.getLines().toList)
    println("lines count: " + entriesList.size)
    timed(
      entriesList.map(_.split(",")).map(l => Transaction(l(1).toDouble, l(2).toDouble, l(0).toLong, Currency.withName(l(3).toLowerCase), l(4), l(5)))
    )
    println("Buffered run")
    timed(Source.fromFile(filename).getLines().foreach(
      s => {
        val l = s.split(",")
        Transaction(l(1).toDouble, l(2).toDouble, l(0).toLong, Currency.withName(l(3).toLowerCase), l(4), l(5))
      }
    ))
  }

  def timed[A](block: => A) = {
    val t0 = System.currentTimeMillis
    val result = block
    println("took " + (System.currentTimeMillis - t0) + "ms")
    result
  }

  def generateFakeData = {
    val writer = new PrintWriter(new File("fakeData.csv"))
    val rnd = new Random()

    for (a <- 1 to 1000000) {
      writer.write(a + "," + (rnd.nextInt(150) + 100) + "," + (rnd.nextInt(30) + 1) + "," + "USD" + "," + "buyer" + "," + "seller" + "\n")
    }
  }
}