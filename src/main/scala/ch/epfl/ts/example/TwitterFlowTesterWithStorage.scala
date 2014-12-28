package ch.epfl.ts.example

import akka.actor._
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.TwitterFetchComponent
import ch.epfl.ts.component.persist.TweetPersistanceComponent
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Tweet

object TwitterFlowTesterWithStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("DataSourceSystem")

    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val persistor = builder.createRef(Props(classOf[TweetPersistanceComponent], "twitter-db"))
    val fetcher = builder.createRef(Props(classOf[TwitterFetchComponent], "twitter-fetcher"))

    fetcher.addDestination(printer, classOf[Tweet])
    fetcher.addDestination(persistor, classOf[Tweet])

    builder.start
  }
}