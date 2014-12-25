package ch.epfl.ts.test

import ch.epfl.ts.data.Order
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.OrderType._
import ch.epfl.ts.impl.OrderPersistorImpl
import ch.epfl.ts.data.Transaction
import ch.epfl.ts.impl.TransactionPersistorImpl
import ch.epfl.ts.impl.TweetPersistorImpl
import ch.epfl.ts.data.Tweet

object PersistorsTest {

  def main(args: Array[String]) {
    println("testing OrderPersistor")
    val order1 = new Order(0, 2.0, 3.0, 1, USD, ASK)
    val order2 = new Order(0, 1.0, 2.0, 2, USD, ASK)
    val ordersPersistor = new OrderPersistorImpl("test")
    ordersPersistor.init()
    ordersPersistor.save(order1)
    ordersPersistor.save(order2)
    val orders = ordersPersistor.loadBatch(0, 2)
    orders.map(a => println(a))
    ordersPersistor.clearAll
    
    println("testing TransactionPersistor")
    val trans1 = new Transaction(33, 41, 1, USD, "Bob", "Bill")
    val trans2 = new Transaction(221, 23, 2, USD, "Jack", "Billy")
    val transList = trans1 :: trans2 :: Nil
    val transPersistor = new TransactionPersistorImpl
    transPersistor.init()
    transPersistor.save(transList)
    val retrievedTrans = transPersistor.loadBatch(0, 100)
    retrievedTrans.map { x => println(x) }
    transPersistor.clearAll
    
    println("testing TweetPersistor")
    val t1 = new Tweet(1, "blabla bitcoiiin", -1, "/docs", "dude")
    val t2 = new Tweet(10, "bitcoin great", 1, "/etc", "bob")
    val tweetPersistor = new TweetPersistorImpl
    tweetPersistor.init()
    tweetPersistor.save(t1)
    tweetPersistor.save(t2)
    val retrievedTweets = tweetPersistor.loadBatch(0, 11)
    retrievedTweets.map { x => println(x) }
    tweetPersistor.clearAll
  }

}