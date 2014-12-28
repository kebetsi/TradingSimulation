package ch.epfl.main

import akka.actor.ActorSystem
object TwitterTest {

  def main(args: Array[String]) {

//    val system = ActorSystem("marketSystem")
//    val twitterer = system.actorOf(Props(new TwitterActor(null)), "market")
//    twitterer ! "start"
    
    val system = ActorSystem("liveTweetSystem")
//    val twitterPushFetchActor = system.actorOf(Props(classOf[PushFetchActor[T]], e, dest))
  }
}