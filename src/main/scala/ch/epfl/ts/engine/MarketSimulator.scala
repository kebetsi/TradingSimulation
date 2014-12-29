package ch.epfl.ts.engine

import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.data.Transaction
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.TreeSet

/**
 *  message used to print the books contents (since we use PriotityQueues, it's the heap order)
 */
case class PrintBooks()

/**
 * message sent to retrieve the contents of ask and bid orders books
 */
case class RetrieveBooks()

/**
 * a tuple containing the bid and ask orders books and the last trading price
 */
case class Books(bids: TreeSet[BidOrder], asks: TreeSet[AskOrder], tradingPrice: Double)

class MarketSimulator extends Actor {

  var dest: List[ActorRef] = Nil

  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv

  // when used on TreeSet, head() and iterator() provide increasing order
  def asksOrdering = new Ordering[AskOrder] {
    def compare(first: AskOrder, second: AskOrder): Int =
      if (first.price > second.price) 1 else if (first.price < second.price) -1 else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  // when used on TreeSet, head() and iterator() provide decreasing order
  def bidsOrdering = new Ordering[BidOrder] {
    def compare(first: BidOrder, second: BidOrder): Int =
      if (first.price > second.price) -1 else if (first.price < second.price) 1 else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  val bidOrdersBook = new TreeSet[BidOrder]()(bidsOrdering)
  val askOrdersBook = new TreeSet[AskOrder]()(asksOrdering)

  def handleNewOrder(newOrder: EngineOrder) {
    newOrder match {
      case bid: BidOrder => {
        println("Market: got bid: " + bid)

        if (askOrdersBook.isEmpty) {
          println("Market: ask orders book empty: enqueuing bid")
          bidOrdersBook += bid
        } else {

          val testedAsk = askOrdersBook.head
          // check if a matching order exists
          if (testedAsk.price <= bid.price) {

            // perfect match
            if (testedAsk.quantity == bid.quantity) {
              println("Market: perfect match with " + testedAsk)
              // create transaction
              dest.map { _ ! new Transaction(bid.price, bid.quantity, bid.timestamp, bid.whatC, bid.uid, bid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order
              println("removing order: " + askOrdersBook.head + " from ask orders book.")
              askOrdersBook -= askOrdersBook.head
              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = bid.price

            } else if (testedAsk.quantity > bid.quantity) {
              println("Market: bid quantity inferior - cutting matched order")
              // create transaction
              dest.map { _ ! new Transaction(bid.price, bid.quantity, bid.timestamp, bid.whatC, bid.uid, bid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order and reinput it with updated volume
              println("removing order: " + askOrdersBook.head + " from ask orders book. enqueuing same ask with " + (testedAsk.quantity - bid.quantity) + " volume.")
              askOrdersBook -= askOrdersBook.head
              askOrdersBook += new AskOrder(testedAsk.uid, testedAsk.oid, testedAsk.timestamp, testedAsk.whatC, testedAsk.price, testedAsk.quantity - bid.quantity, testedAsk.withC)

              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = bid.price
            } else {
              println("Market: bid quantity superior - gonna continue ")
              // create transaction
              dest.map { _ ! new Transaction(bid.price, testedAsk.quantity, bid.timestamp, bid.whatC, bid.uid, bid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order
              println("removing order: " + askOrdersBook.head + " from ask orders book.")
              askOrdersBook -= askOrdersBook.head
              // call handleNewOrder on bid with updated volume
              handleNewOrder(new BidOrder(bid.uid, bid.oid, bid.timestamp, bid.whatC, bid.price, bid.quantity - testedAsk.quantity, bid.withC))
              // update price
              tradingPrice = bid.price
            }
            // no match found
          } else {
            println("Market: no match found - enqueuing")
            // enqueue
            bidOrdersBook += bid
          }
        }
      }
      case ask: AskOrder => {
        println("Market: got ask: " + ask)

        if (bidOrdersBook.isEmpty) {
          println("Market: bid orders book empty: enqueuing ask")
          askOrdersBook += ask
        } else {
          // check if a matching order exists
          val testedBid = bidOrdersBook.head

          if (testedBid.price >= ask.price) {
            // perfect match
            if (testedBid.quantity == ask.quantity) {
              println("Market: perfect match with " + testedBid)
              // create transaction
              dest.map { _ ! new Transaction(ask.price, ask.quantity, ask.timestamp, ask.whatC, testedBid.uid, testedBid.oid, ask.uid, ask.oid) }
              // remove matched ask order
              println("removing order: " + bidOrdersBook.head + " from bid orders book.")
              bidOrdersBook -= bidOrdersBook.head
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = ask.price
            } else if (testedBid.quantity > ask.quantity) {
              println("Market: ask quantity inferior - cutting matched order")
              // create transaction
              dest.map { _ ! new Transaction(ask.price, ask.quantity, ask.timestamp, ask.whatC, testedBid.uid, testedBid.oid, ask.uid, ask.oid) }
              // remove matched bid order and reinput it with updated volume
              println("removing order: " + bidOrdersBook.head + " from bid orders book. enqueuing same ask with " + (testedBid.quantity - ask.quantity) + " volume.")
              bidOrdersBook -= bidOrdersBook.head
              bidOrdersBook += new BidOrder(testedBid.uid, testedBid.oid, testedBid.timestamp, testedBid.whatC, testedBid.price, testedBid.quantity - ask.quantity, testedBid.withC)
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = ask.price
            } else {
              println("Market: ask quantity superior - gonna continue ")
              // create transaction
              dest.map { _ ! new Transaction(ask.price, testedBid.quantity, ask.timestamp, ask.whatC, testedBid.uid, testedBid.oid, ask.uid, ask.oid) }
              // remove matched bid order
              println("removing order: " + bidOrdersBook.head + " from bid orders book.")
              bidOrdersBook -= bidOrdersBook.head
              // call handleNewOrder on ask with updated volume
              handleNewOrder(new BidOrder(ask.uid, ask.oid, ask.timestamp, ask.whatC, ask.price, ask.quantity - testedBid.quantity, ask.withC))
              // update price
              tradingPrice = ask.price
            }
            // no match found
          } else {
            println("Market: no match found - enqueuing")
            // enqueue
            askOrdersBook += ask
          }
        }
      }
      case del: DelOrder => {
        println("Market: got Delete: " + del)
        // look in bids
        bidOrdersBook.find { x => x.oid == del.oid } match {
          case bidToDelete: Some[BidOrder] => {
            println("deleted from Bids")
            bidOrdersBook -= bidToDelete.get
          }
          case _ => {
            // look in asks
            askOrdersBook.find { x => x.oid == del.oid } match {
              case askToDelete: Some[AskOrder] => {
                println("deleted from Asks")
                askOrdersBook -= askToDelete.get
              }
              case _ =>
            }
          }
        }
      }
    }
  }

  def receive = {

    case order: EngineOrder    => handleNewOrder(order)

    case newListener: ActorRef => dest = newListener :: dest

    case PrintBooks => {
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + askOrdersBook)
      println("Bid Orders Book: " + bidOrdersBook)
    }

    case RetrieveBooks => {
      sender ! Books(bidOrdersBook.asInstanceOf[TreeSet[BidOrder]], askOrdersBook.asInstanceOf[TreeSet[AskOrder]], tradingPrice)
    }

    case _ => println("got unknown")
  }
}
