package ch.epfl.ts.test

import akka.actor.ActorSystem
import ch.epfl.bigdata.btc.crawler.twitter.TwitterActor
import akka.actor.Props

object TwitterTest {

  def main(args: Array[String]) {

    val system = ActorSystem("marketSystem")
    val twitterer = system.actorOf(Props(new TwitterActor(null)), "market")
    twitterer ! "start"
  }
}