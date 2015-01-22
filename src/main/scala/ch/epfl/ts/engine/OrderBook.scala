package ch.epfl.ts.engine

import ch.epfl.ts.data.Order

import scala.collection.mutable.{HashMap => MHashMap, TreeSet => MTreeSet}

/**
 * Container for the Order Book
 */
class PartialOrderBook(val comparator: Ordering[Order]) {
  val book = MTreeSet[Order]()(comparator)
  val bookMap = MHashMap[Long, Order]()

  def delete(o: Order): Boolean = bookMap remove o.oid map { r => book remove r; true} getOrElse {
    false
  }

  def insert(o: Order): Unit = {
    bookMap update(o.oid, o)
    book add o
  }

  def isEmpty = book.isEmpty

  def head = book.head

  def size = book.size
}

class OrderBook(val bids: PartialOrderBook, val asks: PartialOrderBook) {

  def delete(o: Order): Unit = if (!bids.delete(o)) asks.delete(o)

  def insertAskOrder(o: Order): Unit = asks.insert(o)

  def insertBidOrder(o: Order): Unit = bids.insert(o)

  //def getOrderById(oid: Long): Option[Order] = bids.bookMap.get(oid)
}

object OrderBook {
  def apply(bidComparator: Ordering[Order], askComparator: Ordering[Order]): OrderBook =
    new OrderBook(new PartialOrderBook(bidComparator), new PartialOrderBook(askComparator))
}


