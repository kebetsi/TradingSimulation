package ch.epfl.main

import ch.epfl.ts.component.persist.{ OrderPersistor, TransactionPersistor, TweetPersistor }
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.data.{ Order, Transaction, Tweet, DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder }
import ch.epfl.ts.data.MarketAskOrder

object PersistorsTest {

  def main(args: Array[String]) {
//    basicTest
        financeOrdersTest
  }

  def financeOrdersTest = {
    val persistor = new OrderPersistor("finance")
    persistor.init()
    persistor.loadBatch(25210389, 25211389).map { x => println(x) }
  }

  def basicTest = {
    println("testing OrderPersistor")
    val order1 = DelOrder(1, 0, 123, DEF, DEF, 0.0, 0.0)
    val order2 = LimitBidOrder(2, 1, 124, BTC, USD, 20.0, 50.0)
    val order3 = LimitAskOrder(3, 1, 125, BTC, USD, 30.0, 60.0)
    val order4 = MarketAskOrder(4, 2, 142, BTC, USD, 20, 0)
    val order5 = MarketBidOrder(5, 2, 144, BTC, USD, 30, 0)
    val ordersPersistor = new OrderPersistor("test")
    ordersPersistor.init()
    ordersPersistor.save(order1)
    ordersPersistor.save(order2)
    ordersPersistor.save(order3 :: order4 :: order5 :: Nil)
    val orders = ordersPersistor.loadBatch(0, 200)
    println("retrieved " + orders.size + " orders.")
    orders.map(a => println(a))
    ordersPersistor.clearAll

    println("testing TransactionPersistor")
    val trans1 = Transaction(33, 41, 1, BTC, USD, 1, 1, 2, 2)
    val trans2 = Transaction(221, 23, 2, BTC, USD, 3, 3, 4, 4)
    val transList = trans1 :: trans2 :: Nil
    val transPersistor = new TransactionPersistor("test")
    transPersistor.init()
    transPersistor.save(transList)
    val retrievedTrans = transPersistor.loadBatch(0, 100)
    retrievedTrans.map { x => println(x) }
    transPersistor.clearAll

    println("testing TweetPersistor")
    val t1 = new Tweet(1, "blabla bitcoiiin", -1, "/docs", "dude")
    val t2 = new Tweet(10, "bitcoin great", 1, "/etc", "bob")
    val tweetPersistor = new TweetPersistor("PersistorsTest-tweet-db")
    tweetPersistor.init()
    tweetPersistor.save(t1)
    tweetPersistor.save(t2)
    val retrievedTweets = tweetPersistor.loadBatch(0, 11)
    retrievedTweets.map { x => println(x) }
    tweetPersistor.clearAll
  }

}