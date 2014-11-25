package ch.epfl.ts.benchmark

import akka.actor.{ActorRef, Actor}
import ch.epfl.ts.data.{Currency, Transaction}

import scala.io.Source

class FileFetchActor(destination: ActorRef, filename: String) extends Actor {
  def receive = {
    case Run => readAndSend
  }
  def readAndSend = {
    val startTime = System.currentTimeMillis
    val source = Source.fromFile(filename)
    val lines = source.getLines().toList
    lines.map(_.split(",")).map(
      l => destination ! Transaction(l(1).toDouble, l(2).toDouble, l(0).toLong, Currency.withName(l(3).toLowerCase), l(4), l(5))
    )
    val endTime = System.currentTimeMillis

  }
}

case class Run()
