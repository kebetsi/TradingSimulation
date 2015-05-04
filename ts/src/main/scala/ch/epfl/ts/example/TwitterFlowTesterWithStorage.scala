package ch.epfl.ts.example

import akka.actor._
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.TwitterFetchComponent
import ch.epfl.ts.component.persist.{Persistor, TweetPersistor}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Tweet

import scala.reflect.ClassTag

object TwitterFlowTesterWithStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("TwitterPrintSystem")

    // Initialize the Interface to DB
    val tweetPersistor = new TweetPersistor("twitter-db")

    // Create Components
    val printer = builder.createRef(Props(classOf[Printer]), "printer")
    val persistor = builder.createRef(Props(classOf[Persistor[Tweet]], tweetPersistor, implicitly[ClassTag[Tweet]]), "tweet-persistor")
    val fetcher = builder.createRef(Props(classOf[TwitterFetchComponent]), "twitter-fetcher")

    // Create the connections
    fetcher->(Seq(printer, persistor), classOf[Tweet])

    // Start the system
    builder.start
  }
}