package ch.epfl.ts.engine

import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.data.Currency.Currency
import ch.epfl.ts.data.Transaction
import akka.actor.Actor

case class PrintBooks()

class MarketSimulator extends Actor {

  def priceOrdering = new Ordering[Order] {
    def compare(first: Order, second: Order): Int = {
      if (first.price > second.price) {
        return 1
      } else if (first.price < second.price) {
        return -1
      } else {
        return 0
      }
    }
  }

  def inversePriceOrdering = new Ordering[Order] {
    def compare(first: Order, second: Order): Int = {
      if (first.price > second.price) {
        return -1
      } else if (first.price < second.price) {
        return 1
      } else {
        return 0
      }
    }
  }

  var bidOrdersBook = new PriorityQueue()(priceOrdering)
  var askOrdersBook = new PriorityQueue()(inversePriceOrdering)

  def handleMatch(tested: Order, possibleMatch: Option[Order], targetQueue: PriorityQueue[Order], testedQueue: PriorityQueue[Order]) = possibleMatch match {
    case Some(s) => {
      println("found match")
      println("creating transaction: " + new Transaction(s.price, s.quantity, 999, s.whatC, "buya", "sella"))
      // remove matched ask order
      println("removing order: " + targetQueue.dequeue() + " from " + targetQueue)
    }
    case None => {
      println("enqueueing: " + tested)
      testedQueue.enqueue(tested)
    }
  }

  def receive = {

    case bid: BidOrder => {
      println("got bid: " + bid)

      if (askOrdersBook.isEmpty) {
        println("asks are empty")
      } else {
        println("going try to match: " + askOrdersBook.head + " and " + bid)
      }

      // find ask order with a price lower than the bid
      handleMatch(bid, askOrdersBook.find { ask => ask.price <= bid.price }, askOrdersBook, bidOrdersBook)

    }
    case ask: AskOrder => {
      println("got ask: " + ask)

      if (bidOrdersBook.isEmpty) {
        println("bids are empty")
      } else {
        println("going try to match: " + bidOrdersBook.head + " and " + ask)
      }

      // find bid order with a price higher than the ask
      handleMatch(ask, bidOrdersBook.find { bid => bid.price >= ask.price }, bidOrdersBook, askOrdersBook)
    }

    case PrintBooks => {
      println("Ask Orders Book: " + askOrdersBook)
      println("Bid Orders Book: " + bidOrdersBook)
    }

    case _ => {
      println("got unknown")
    }
  }

}
