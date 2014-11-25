package ch.epfl.ts.benchmark

import scala.io.Source
import java.io._
import scala.util.Random

import ch.epfl.ts.data.{Transaction, Currency}

object Native {

  def main(args: Array[String]) = {

    val source = Source.fromFile("fakeData.csv")
    val entriesList = timed(source.getLines().toList)
    val test = entriesList.head
    println("first entry: " + entriesList.head)
    var retrievedTransactions: List[Transaction] = List()
    timed(entriesList.foreach { a => val x = a.split(","); retrievedTransactions = new Transaction(x(1).toDouble, x(2).toDouble, x(0).toLong, Currency.withName(x(3).toLowerCase()), x(4), x(5)) :: retrievedTransactions})
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