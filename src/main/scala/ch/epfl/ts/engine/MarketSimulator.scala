package ch.epfl.ts.engine

import scala.collection.mutable.PriorityQueue
import ch.epfl.ts.data.Transaction
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.TreeSet
import ch.epfl.ts.component.Component

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

class MarketSimulator(rules: MarketRules) extends Component {

  /**
   * the price at which the last transaction was executed
   */
  var tradingPrice: Double = 185000.0 // set for SobiTrader when using with finance.csv

  val bidOrdersBook = new TreeSet[LimitBidOrder]()(rules.bidOrdersBookPriority)
  val askOrdersBook = new TreeSet[LimitAskOrder]()(rules.askOrdersBookPriority)

  override def receiver = {

    case limitBid: LimitBidOrder => {
      tradingPrice = rules.matchingFunction(limitBid, bidOrdersBook.asInstanceOf[TreeSet[EngineOrder]], askOrdersBook.asInstanceOf[TreeSet[EngineOrder]], this.send[Transaction], (a, b) => a <= b, tradingPrice, (limitBid, bidOrdersBook) => { bidOrdersBook += limitBid; println("order enqueued") })
    }
    case limitAsk: LimitAskOrder => {
      tradingPrice = rules.matchingFunction(limitAsk, askOrdersBook.asInstanceOf[TreeSet[EngineOrder]], bidOrdersBook.asInstanceOf[TreeSet[EngineOrder]], this.send[Transaction], (a, b) => a >= b, tradingPrice, (limitAsk, askOrdersBook) => { askOrdersBook += limitAsk; println("order enqueued") })
    }
    case marketBid: MarketBidOrder => {
      tradingPrice = rules.matchingFunction(marketBid, bidOrdersBook.asInstanceOf[TreeSet[EngineOrder]], askOrdersBook.asInstanceOf[TreeSet[EngineOrder]], this.send[Transaction], (a, b) => true, tradingPrice, (marketBid, bidOrdersBook) => (println("market order discarded")))
    }
    case marketAsk: MarketAskOrder => {
      tradingPrice = rules.matchingFunction(marketAsk, askOrdersBook.asInstanceOf[TreeSet[EngineOrder]], bidOrdersBook.asInstanceOf[TreeSet[EngineOrder]], this.send[Transaction], (a, b) => true, tradingPrice, (marketAsk, askOrdersBook) => (println("market order discarded")))
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

    case PrintBooks => {
      // print shows heap order (binary tree)
      println("Ask Orders Book: " + askOrdersBook)
      println("Bid Orders Book: " + bidOrdersBook)
    }

    case RetrieveBooks => {
      sender ! Books(bidOrdersBook, askOrdersBook, tradingPrice)
    }

    case _ => println("Market: got unknown")
  }
}
