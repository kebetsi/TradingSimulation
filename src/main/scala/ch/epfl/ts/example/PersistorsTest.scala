package ch.epfl.ts.example

import ch.epfl.ts.component.persist.{OrderPersistor, TransactionPersistor, TweetPersistor}
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Transaction, Tweet}

/**
 * describes persistors usage
 */
object PersistorsTest {

  def main(args: Array[String]) {
//    basicTest
        financeOrdersTest
  }

  /**
   * loads orders from previously loaded finance.csv
   */
  def financeOrdersTest = {
    val persistor = new OrderPersistor("finance")
    persistor.init()
    persistor.loadBatch(25210389, 25243295).map { x => println(x) }
  }

  /**
   * tests Order-, Transaction- and TweetPersistors with generated data
   */
  def basicTest = {
    println("testing OrderPersistor")
    // generate orders
    val order1 = DelOrder(1, 0, 123, DEF, DEF, 0.0, 0.0)
    val order2 = LimitBidOrder(2, 1, 124, BTC, USD, 20.0, 50.0)
    val order3 = LimitAskOrder(3, 1, 125, BTC, USD, 30.0, 60.0)
    val order4 = MarketAskOrder(4, 2, 142, BTC, USD, 20, 0)
    val order5 = MarketBidOrder(5, 2, 144, BTC, USD, 30, 0)
    // init OrderPersistor
    val ordersPersistor = new OrderPersistor("test")
    ordersPersistor.init()
    // save single
    ordersPersistor.save(order1)
    ordersPersistor.save(order2)
    // save batch
    ordersPersistor.save(order3 :: order4 :: order5 :: Nil)
    // load & print
    val orders = ordersPersistor.loadBatch(0, 200)
    println("retrieved " + orders.size + " orders.")
    orders.map(a => println(a))
    // clean
    ordersPersistor.clearAll

    println("testing TransactionPersistor")
    // generate transactions
    val trans1 = Transaction(1, 33, 41, 1, BTC, USD, 1, 1, 2, 2)
    val trans2 = Transaction(2, 221, 23, 2, BTC, USD, 3, 3, 4, 4)
    val transList = trans1 :: trans2 :: Nil
    // init TransactionPersistor
    val transPersistor = new TransactionPersistor("test")
    transPersistor.init()
    // save batch
    transPersistor.save(transList)
    // load & print
    val retrievedTrans = transPersistor.loadBatch(0, 100)
    retrievedTrans.map { x => println(x) }
    // clean
    transPersistor.clearAll

    println("testing TweetPersistor")
    // generate tweets
    val t1 = new Tweet(1, "blabla bitcoiiin", -1, "/docs", "dude")
    val t2 = new Tweet(10, "bitcoin great", 1, "/etc", "bob")
    // init TweetPersistor
    val tweetPersistor = new TweetPersistor("PersistorsTest-tweet-db")
    tweetPersistor.init()
    // save
    tweetPersistor.save(t1)
    tweetPersistor.save(t2)
    // load & print
    val retrievedTweets = tweetPersistor.loadBatch(0, 11)
    retrievedTweets.map { x => println(x) }
    // clean
    tweetPersistor.clearAll
  }

}