package ch.epfl.ts.engine

import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.data.Currency.Currency
import ch.epfl.ts.data.Transaction
import akka.actor.Actor
import com.sun.javafx.binding.SelectBinding.AsObject

/**
 *  message used to print the books contents (since we use PriotityQueues, it's the heap order)
 */
case class PrintBooks()

class MarketSimulator extends Actor {

  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 123456789

  def decreasingPriceOrdering = new Ordering[EngineOrder] {
    def compare(first: EngineOrder, second: EngineOrder): Int =
      if (first.price > second.price) 1 else if (first.price < second.price) -1 else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  def increasingPriceOrdering = new Ordering[EngineOrder] {
    def compare(first: EngineOrder, second: EngineOrder): Int =
      if (first.price > second.price) -1 else if (first.price < second.price) 1 else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  var bidOrdersBook = new PriorityQueue()(decreasingPriceOrdering)
  var askOrdersBook = new PriorityQueue()(increasingPriceOrdering)

  def handleMatch(tested: EngineOrder, possibleMatch: Option[EngineOrder], targetQueue: PriorityQueue[EngineOrder], testedQueue: PriorityQueue[EngineOrder]) = possibleMatch match {
    case Some(s) => {
      println("found match")
      println("creating transaction: " + new Transaction(s.price, s.quantity, 999, s.whatC, 1, 1, 2, 2))
      // remove matched ask order
      println("removing order: " + targetQueue.dequeue() + " from " + targetQueue)
    }
    case None => {
      println("enqueueing: " + tested)
      testedQueue.enqueue(tested)
    }
  }

  def handleNewOrder(newOrder: EngineOrder) {
    newOrder match {
      case bid: BidOrder => {
        println("Market: got bid: " + bid)

        if (askOrdersBook.isEmpty) {
          println("Market: ask orders book empty: enqueuing bid")
          bidOrdersBook.enqueue(bid)
        } else {

          val testedAsk = askOrdersBook.head
          // check if a matching order exists
          if (testedAsk.price <= bid.price) {

            // perfect match
            if (testedAsk.quantity == bid.quantity) {
              println("Market: perfect match with " + testedAsk)
              // create transaction
              new Transaction(bid.price, bid.quantity, bid.timestamp, bid.whatC, bid.uid, bid.oid, testedAsk.uid, testedAsk.oid)
              // remove matched ask order
              println("removing order: " + askOrdersBook.dequeue() + " from ask orders book.")
              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = bid.price

            } else if (testedAsk.quantity > bid.quantity) {
              println("Market: bid quantity inferior - cutting matched order")
              // create transaction
              new Transaction(bid.price, bid.quantity, bid.timestamp, bid.whatC, bid.uid, bid.oid, testedAsk.uid, testedAsk.oid)
              // remove matched ask order and reinput it with updated volume
              println("removing order: " + askOrdersBook.dequeue() + " from ask orders book. enqueuing same ask with " + (testedAsk.quantity - bid.quantity) + " volume.")
              askOrdersBook.enqueue(new AskOrder(testedAsk.uid, testedAsk.oid, testedAsk.timestamp, testedAsk.whatC, testedAsk.price, testedAsk.quantity - bid.quantity, testedAsk.withC))
              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = bid.price
            } else {
              println("Market: bid quantity superior - gonna continue ")
              // create transaction
              new Transaction(bid.price, testedAsk.quantity, bid.timestamp, bid.whatC, bid.uid, bid.oid, testedAsk.uid, testedAsk.oid)
              // remove matched ask order
              println("removing order: " + askOrdersBook.dequeue() + " from ask orders book.")
              // call handleNewOrder on bid with updated volume
              handleNewOrder(new BidOrder(bid.uid, bid.oid, bid.timestamp, bid.whatC, bid.price, bid.quantity - testedAsk.quantity, bid.withC))
              // update price
              tradingPrice = bid.price
            }
            // no match found
          } else {
            println("Market: no match found - enqueuing")
            // enqueue
            bidOrdersBook.enqueue(bid)
          }
        }
      }
      case ask: AskOrder => {
        println("Market: got ask")

        if (bidOrdersBook.isEmpty) {
          println("Market: bid orders book empty: enqueuing ask")
          askOrdersBook.enqueue(ask)
        } else {
          // check if a matching order exists
          val testedBid = bidOrdersBook.head

          if (testedBid.price >= ask.price) {
            // perfect match
            if (testedBid.quantity == ask.quantity) {
              println("Market: perfect match with " + testedBid)
              // create transaction
              new Transaction(ask.price, ask.quantity, ask.timestamp, ask.whatC, testedBid.uid, testedBid.oid, ask.uid, ask.oid)
              // remove matched ask order
              println("removing order: " + bidOrdersBook.dequeue() + " from bid orders book.")
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = ask.price
            } else if (testedBid.quantity > ask.quantity) {
              println("Market: ask quantity inferior - cutting matched order")
              // create transaction
              new Transaction(ask.price, ask.quantity, ask.timestamp, ask.whatC, testedBid.uid, testedBid.oid, ask.uid, ask.oid)
              // remove matched bid order and reinput it with updated volume
              println("removing order: " + bidOrdersBook.dequeue() + " from bid orders book. enqueuing same ask with " + (testedBid.quantity - ask.quantity) + " volume.")
              askOrdersBook.enqueue(new BidOrder(testedBid.uid, testedBid.oid, testedBid.timestamp, testedBid.whatC, testedBid.price, testedBid.quantity - ask.quantity, testedBid.withC))
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = ask.price
            } else {
              println("Market: ask quantity superior - gonna continue ")
              // create transaction
              new Transaction(ask.price, testedBid.quantity, ask.timestamp, ask.whatC, testedBid.uid, testedBid.oid, ask.uid, ask.oid)
              // remove matched bid order
              println("removing order: " + bidOrdersBook.dequeue() + " from bid orders book.")
              // call handleNewOrder on ask with updated volume
              handleNewOrder(new BidOrder(ask.uid, ask.oid, ask.timestamp, ask.whatC, ask.price, ask.quantity - testedBid.quantity, ask.withC))
              // update price
              tradingPrice = ask.price
            }
            // no match found
          } else {
            println("Market: no match found - enqueuing")
            // enqueue
            askOrdersBook.enqueue(ask)
          }
        }
      }
      case del: DelOrder => {
        println("Market: got Delete")
        // find order with matching id (need to search both books)
        //        var found = false
        //        val iter = askOrdersBook.iterator
        //        while (iter.hasNext && !found) {
        //          var cur = iter.next()
        //          cur.
        //          if (iter.next().oid == del.oid)
        //        }
        //        askOrdersBook.find { x => x.oid == del.oid } match {
        //          case Some(s) =>
        //        }

        // delete it
        // to delete it - increase its priority to max then dequeue
        // do something if not found?
      }
    }
  }

  def receive = {

    case order: EngineOrder => handleNewOrder(order)

    //    case bid: BidOrder => {
    //      println("got bid: " + bid)
    //
    //      if (askOrdersBook.isEmpty) {
    //        println("asks are empty")
    //      } else {
    //        println("going try to match: " + askOrdersBook.head + " and " + bid)
    //      }
    //
    //      // find ask order with a price lower than the bid
    //      //      handleMatch(bid, askOrdersBook.find { ask => ask.price <= bid.price }, askOrdersBook, bidOrdersBook)
    //
    //    }
    //    case ask: AskOrder => {
    //      println("got ask: " + ask)
    //
    //      if (bidOrdersBook.isEmpty) {
    //        println("bids are empty")
    //      } else {
    //        println("going try to match: " + bidOrdersBook.head + " and " + ask)
    //      }
    //
    //
    //      // find bid order with a price higher than the ask
    //      handleMatch(ask, bidOrdersBook.find { bid => bid.price >= ask.price }, bidOrdersBook, askOrdersBook)
    //    }

    case PrintBooks => {
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + askOrdersBook)
      println("Bid Orders Book: " + bidOrdersBook)
    }

    case _ => println("got unknown")
  }
}
