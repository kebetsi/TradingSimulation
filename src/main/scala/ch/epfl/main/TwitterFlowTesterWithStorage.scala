package ch.epfl.main

import akka.actor._
import ch.epfl.ts.data.Tweet

object TwitterFlowTesterWithStorage {
  // Stage 2, used just to print out the result from stage 1
  class Printer extends Actor {
    override def receive = {
      case t: Tweet => println(System.currentTimeMillis, t.toString)
      case _  => 
    }
  }
/*
  def main(args: Array[String]) = {
    val system = ActorSystem("DataSourceSystem")
    val printer = system.actorOf(Props(classOf[Printer]), "instage-printer")
    val persistor = new TweetPersistorImpl // TweetPersistorImpl
    persistor.init()
    val instage = new InStage[Tweet](system, List(printer))
      .withPersistance(persistor)
      .withFetchInterface(new TwitterPushFetcher()).start
  }*/
}