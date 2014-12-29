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
case class Books(bids: TreeSet[LimitBidOrder], asks: TreeSet[LimitAskOrder], tradingPrice: Double)

class MarketSimulator extends Actor {

  var dest: List[ActorRef] = Nil

  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv

  // when used on TreeSet, head() and iterator() provide increasing order
  def asksOrdering = new Ordering[LimitAskOrder] {
    def compare(first: LimitAskOrder, second: LimitAskOrder): Int =
      if (first.price > second.price) 1 else if (first.price < second.price) -1 else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  // when used on TreeSet, head() and iterator() provide decreasing order
  def bidsOrdering = new Ordering[LimitBidOrder] {
    def compare(first: LimitBidOrder, second: LimitBidOrder): Int =
      if (first.price > second.price) -1 else if (first.price < second.price) 1 else {
        if (first.timestamp < second.timestamp) 1 else if (first.timestamp > second.timestamp) -1 else 0
      }
  }

  val bidOrdersBook = new TreeSet[LimitBidOrder]()(bidsOrdering)
  val askOrdersBook = new TreeSet[LimitAskOrder]()(asksOrdering)

  def handleNewOrder(newOrder: EngineOrder) {
    newOrder match {
      case limitBid: LimitBidOrder => {
        println("Market: got limit bid: " + limitBid)

        if (askOrdersBook.isEmpty) {
          println("Market: ask orders book empty: enqueuing bid")
          bidOrdersBook += limitBid
        } else {

          val testedAsk = askOrdersBook.head
          // check if a matching order exists
          if (testedAsk.price <= limitBid.price) {

            // perfect match
            if (testedAsk.quantity == limitBid.quantity) {
              println("Market: perfect match with " + testedAsk)
              // create transaction
              dest.map { _ ! new Transaction(limitBid.price, limitBid.quantity, limitBid.timestamp, limitBid.whatC, limitBid.uid, limitBid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order
              println("removing order: " + askOrdersBook.head + " from ask orders book.")
              askOrdersBook -= askOrdersBook.head
              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = limitBid.price

            } else if (testedAsk.quantity > limitBid.quantity) {
              println("Market: bid quantity inferior - cutting matched order")
              // create transaction
              dest.map { _ ! new Transaction(limitBid.price, limitBid.quantity, limitBid.timestamp, limitBid.whatC, limitBid.uid, limitBid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order and reinput it with updated volume
              println("removing order: " + askOrdersBook.head + " from ask orders book. enqueuing same ask with " + (testedAsk.quantity - limitBid.quantity) + " volume.")
              askOrdersBook -= askOrdersBook.head
              askOrdersBook += new LimitAskOrder(testedAsk.uid, testedAsk.oid, testedAsk.timestamp, testedAsk.whatC, testedAsk.price, testedAsk.quantity - limitBid.quantity, testedAsk.withC)

              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = limitBid.price
            } else {
              println("Market: bid quantity superior - gonna continue ")
              // create transaction
              dest.map { _ ! new Transaction(limitBid.price, testedAsk.quantity, limitBid.timestamp, limitBid.whatC, limitBid.uid, limitBid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order
              println("removing order: " + askOrdersBook.head + " from ask orders book.")
              askOrdersBook -= askOrdersBook.head
              // call handleNewOrder on bid with updated volume
              handleNewOrder(new LimitBidOrder(limitBid.uid, limitBid.oid, limitBid.timestamp, limitBid.whatC, limitBid.price, limitBid.quantity - testedAsk.quantity, limitBid.withC))
              // update price
              tradingPrice = limitBid.price
            }
            // no match found
          } else {
            println("Market: no match found - enqueuing")
            // enqueue
            bidOrdersBook += limitBid
          }
        }
      }
      case limitAsk: LimitAskOrder => {
        println("Market: got limit ask: " + limitAsk)

        if (bidOrdersBook.isEmpty) {
          println("Market: bid orders book empty: enqueuing ask")
          askOrdersBook += limitAsk
        } else {
          // check if a matching order exists
          val testedBid = bidOrdersBook.head

          if (testedBid.price >= limitAsk.price) {
            // perfect match
            if (testedBid.quantity == limitAsk.quantity) {
              println("Market: perfect match with " + testedBid)
              // create transaction
              dest.map { _ ! new Transaction(limitAsk.price, limitAsk.quantity, limitAsk.timestamp, limitAsk.whatC, testedBid.uid, testedBid.oid, limitAsk.uid, limitAsk.oid) }
              // remove matched ask order
              println("removing order: " + bidOrdersBook.head + " from bid orders book.")
              bidOrdersBook -= bidOrdersBook.head
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = limitAsk.price
            } else if (testedBid.quantity > limitAsk.quantity) {
              println("Market: ask quantity inferior - cutting matched order")
              // create transaction
              dest.map { _ ! new Transaction(limitAsk.price, limitAsk.quantity, limitAsk.timestamp, limitAsk.whatC, testedBid.uid, testedBid.oid, limitAsk.uid, limitAsk.oid) }
              // remove matched bid order and reinput it with updated volume
              println("removing order: " + bidOrdersBook.head + " from bid orders book. enqueuing same ask with " + (testedBid.quantity - limitAsk.quantity) + " volume.")
              bidOrdersBook -= bidOrdersBook.head
              bidOrdersBook += new LimitBidOrder(testedBid.uid, testedBid.oid, testedBid.timestamp, testedBid.whatC, testedBid.price, testedBid.quantity - limitAsk.quantity, testedBid.withC)
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = limitAsk.price
            } else {
              println("Market: ask quantity superior - gonna continue ")
              // create transaction
              dest.map { _ ! new Transaction(limitAsk.price, testedBid.quantity, limitAsk.timestamp, limitAsk.whatC, testedBid.uid, testedBid.oid, limitAsk.uid, limitAsk.oid) }
              // remove matched bid order
              println("removing order: " + bidOrdersBook.head + " from bid orders book.")
              bidOrdersBook -= bidOrdersBook.head
              // call handleNewOrder on ask with updated volume
              handleNewOrder(new LimitBidOrder(limitAsk.uid, limitAsk.oid, limitAsk.timestamp, limitAsk.whatC, limitAsk.price, limitAsk.quantity - testedBid.quantity, limitAsk.withC))
              // update price
              tradingPrice = limitAsk.price
            }
            // no match found
          } else {
            println("Market: no match found - enqueuing")
            // enqueue
            askOrdersBook += limitAsk
          }
        }
      }
      case marketBid: MarketBidOrder => {
        println("Market: got market bid: " + marketBid)
        if (askOrdersBook.isEmpty) {
          println("Market: ask orders book empty: discarding market bid order")
        } else {
          val testedAsk = askOrdersBook.head
          // check if a matching order exists
          if (testedAsk.price <= marketBid.price) {

            // perfect match
            if (testedAsk.quantity == marketBid.quantity) {
              println("Market: perfect match with " + testedAsk)
              // create transaction
              dest.map { _ ! new Transaction(testedAsk.price, marketBid.quantity, marketBid.timestamp, marketBid.whatC, marketBid.uid, marketBid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order
              println("removing order: " + askOrdersBook.head + " from ask orders book.")
              askOrdersBook -= askOrdersBook.head
              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = testedAsk.price

            } else if (testedAsk.quantity > marketBid.quantity) {
              println("Market: bid quantity inferior - cutting matched order")
              // create transaction
              dest.map { _ ! new Transaction(testedAsk.price, marketBid.quantity, marketBid.timestamp, marketBid.whatC, marketBid.uid, marketBid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order and reinput it with updated volume
              println("removing order: " + askOrdersBook.head + " from ask orders book. enqueuing same ask with " + (testedAsk.quantity - marketBid.quantity) + " volume.")
              askOrdersBook -= askOrdersBook.head
              askOrdersBook += new LimitAskOrder(testedAsk.uid, testedAsk.oid, testedAsk.timestamp, testedAsk.whatC, testedAsk.price, testedAsk.quantity - marketBid.quantity, testedAsk.withC)

              // do nothing with matched bid - it was executed in the transaction
              // update price
              tradingPrice = testedAsk.price
            } else {
              println("Market: bid quantity superior - gonna continue ")
              // create transaction
              dest.map { _ ! new Transaction(testedAsk.price, testedAsk.quantity, marketBid.timestamp, marketBid.whatC, marketBid.uid, marketBid.oid, testedAsk.uid, testedAsk.oid) }
              // remove matched ask order
              println("removing order: " + askOrdersBook.head + " from ask orders book.")
              askOrdersBook -= askOrdersBook.head
              // call handleNewOrder on bid with updated volume
              handleNewOrder(new MarketBidOrder(marketBid.uid, marketBid.oid, marketBid.timestamp, marketBid.whatC, testedAsk.price, marketBid.quantity - testedAsk.quantity, marketBid.withC))
              // update price
              tradingPrice = testedAsk.price
            }
            // no match found
          }
        }
      }
      
      case marketAsk: MarketAskOrder => {
        println("Market: got market ask: " + marketAsk)

        if (bidOrdersBook.isEmpty) {
          println("Market: bid orders book empty: discarding ask")
        } else {
          // check if a matching order exists
          val testedBid = bidOrdersBook.head

          if (testedBid.price >= marketAsk.price) {
            // perfect match
            if (testedBid.quantity == marketAsk.quantity) {
              println("Market: perfect match with " + testedBid)
              // create transaction
              dest.map { _ ! new Transaction(testedBid.price, marketAsk.quantity, marketAsk.timestamp, marketAsk.whatC, testedBid.uid, testedBid.oid, marketAsk.uid, marketAsk.oid) }
              // remove matched ask order
              println("removing order: " + bidOrdersBook.head + " from bid orders book.")
              bidOrdersBook -= bidOrdersBook.head
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = testedBid.price
            } else if (testedBid.quantity > marketAsk.quantity) {
              println("Market: ask quantity inferior - cutting matched order")
              // create transaction
              dest.map { _ ! new Transaction(testedBid.price, marketAsk.quantity, marketAsk.timestamp, marketAsk.whatC, testedBid.uid, testedBid.oid, marketAsk.uid, marketAsk.oid) }
              // remove matched bid order and reinput it with updated volume
              println("removing order: " + bidOrdersBook.head + " from bid orders book. enqueuing same ask with " + (testedBid.quantity - marketAsk.quantity) + " volume.")
              bidOrdersBook -= bidOrdersBook.head
              bidOrdersBook += new LimitBidOrder(testedBid.uid, testedBid.oid, testedBid.timestamp, testedBid.whatC, testedBid.price, testedBid.quantity - marketAsk.quantity, testedBid.withC)
              // do nothing with matched ask - it was executed in the transaction
              // update price
              tradingPrice = testedBid.price
            } else {
              println("Market: ask quantity superior - gonna continue ")
              // create transaction
              dest.map { _ ! new Transaction(testedBid.price, testedBid.quantity, marketAsk.timestamp, marketAsk.whatC, testedBid.uid, testedBid.oid, marketAsk.uid, marketAsk.oid) }
              // remove matched bid order
              println("removing order: " + bidOrdersBook.head + " from bid orders book.")
              bidOrdersBook -= bidOrdersBook.head
              // call handleNewOrder on ask with updated volume
              handleNewOrder(new MarketAskOrder(marketAsk.uid, marketAsk.oid, marketAsk.timestamp, marketAsk.whatC, testedBid.price, marketAsk.quantity - testedBid.quantity, marketAsk.withC))
              // update price
              tradingPrice = testedBid.price
            }
            // no match found
          }
        }
      }

      case del: DelOrder => {
        println("Market: got Delete: " + del)
        // look in bids
        bidOrdersBook.find { x => x.oid == del.oid } match {
          case bidToDelete: Some[LimitBidOrder] => {
            println("deleted from Bids")
            bidOrdersBook -= bidToDelete.get
          }
          case _ => {
            // look in asks
            askOrdersBook.find { x => x.oid == del.oid } match {
              case askToDelete: Some[LimitAskOrder] => {
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
      sender ! Books(bidOrdersBook, askOrdersBook, tradingPrice)
    }

    case _ => println("got unknown")
  }
}
