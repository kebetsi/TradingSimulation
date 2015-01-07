package ch.epfl.ts.engine

import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.data.{ Message, Transaction, Order, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, DelOrder }
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.TreeSet
import ch.epfl.ts.component.Component

/**
 *  message used to print the books contents (since we use PriotityQueues, it's the heap order)
 */
case class PrintBooks()

class MarketSimulator(rules: MarketRules) extends Component {

  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv

  val bidOrdersBook = new TreeSet[LimitBidOrder]()(rules.bidsOrdering)
  val askOrdersBook = new TreeSet[LimitAskOrder]()(rules.asksOrdering)

  override def receiver = {
    case limitBid: LimitBidOrder => {
      tradingPrice = rules.matchingFunction(limitBid.asInstanceOf[Order], bidOrdersBook.asInstanceOf[TreeSet[Order]], askOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Message], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => { bidOrdersBook += limitBid; send(limitBid); println("MS: order enqueued") })
    }
    case limitAsk: LimitAskOrder => {
      tradingPrice = rules.matchingFunction(limitAsk.asInstanceOf[Order], askOrdersBook.asInstanceOf[TreeSet[Order]], bidOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Message], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => { askOrdersBook += limitAsk; send(limitAsk); println("MS: order enqueued") })
    }
    case marketBid: MarketBidOrder => {
      tradingPrice = rules.matchingFunction(marketBid.asInstanceOf[Order], bidOrdersBook.asInstanceOf[TreeSet[Order]], askOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Message], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => (println("MS: market order discarded")))
    }
    case marketAsk: MarketAskOrder => {
      tradingPrice = rules.matchingFunction(marketAsk.asInstanceOf[Order], askOrdersBook.asInstanceOf[TreeSet[Order]], bidOrdersBook.asInstanceOf[TreeSet[Order]], this.send[Message], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => (println("MS: market order discarded")))
    }

    case del: DelOrder => {
      println("MS: got Delete: " + del)
      send(del)
      // look in bids
      bidOrdersBook.find { x => x.oid == del.oid } match {
        case bidToDelete: Some[LimitBidOrder] => {
          println("MS: order deleted from Bids")
          bidOrdersBook -= bidToDelete.get
        }
        case _ => {
          // look in asks
          askOrdersBook.find { x => x.oid == del.oid } match {
            case askToDelete: Some[LimitAskOrder] => {
              println("MS: order deleted from Asks")
              askOrdersBook -= askToDelete.get
            }
            case _ =>
          }
        }
      }
    }

    case PrintBooks => {
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + askOrdersBook)
      println("Bid Orders Book: " + bidOrdersBook)
    }

    case _ => println("MS: got unknown")
  }
}
