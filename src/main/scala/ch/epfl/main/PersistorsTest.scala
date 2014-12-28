package ch.epfl.main

import ch.epfl.ts.component.persist.{OrderPersistor, TransactionPersistor, TweetPersistor}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.data.{Order, Transaction, Tweet}

object PersistorsTest {

  def main(args: Array[String]) {
//    basicTest
    financeOrdersTest
  }

  def financeOrdersTest = {
    val persistor = new OrderPersistor("finance")
    persistor.init()
    persistor.loadBatch(25210389, 25252541).map { x => println(x) }
  }

  def basicTest = {
    println("testing OrderPersistor")
    val order1 = new Order(0, 2.0, 3.0, 1, USD, ASK)
    val order2 = new Order(0, 1.0, 2.0, 2, USD, ASK)
    val ordersPersistor = new OrderPersistor("test")
    ordersPersistor.init()
    ordersPersistor.save(order1)
    ordersPersistor.save(order2)
    val orders = ordersPersistor.loadBatch(0, 2)
    orders.map(a => println(a))
    ordersPersistor.clearAll

    println("testing TransactionPersistor")
    val trans1 = new Transaction(33, 41, 1, USD, 1, 1, 2, 2)
    val trans2 = new Transaction(221, 23, 2, USD, 3, 3, 4, 4)
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