package ch.epfl.ts.example

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.engine.RevenueComputeFX
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef

class MyHostActor(workers: List[ActorRef]) extends Actor {
  
  // Ping all workers on construction
  workers.foreach {
    w => w ! "Ping!"
  }
  
  override def receive = {
    case s: String => {
      println("MyHostActor received: " + s)
    }
    case m => println("Host received weird: " + m)
  }
  
}

class MyRemoteActor(host: ActorRef) extends Actor {
  
  override def receive = {
    case s: String => {
      println("MyRemoteActor received: " + s)
      host ! "Hi there!"
    }
    case m => println("Host received weird: " + m)
  }
  
}

object RemotingExample {
  def main(args: Array[String]): Unit = {
    
    val remotingConfig = ConfigFactory.parseString(
"""
akka.actor.provider = "akka.remote.RemoteActorRefProvider"
akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
akka.remote.netty.tcp.port = 40048
akka.remote.netty.tcp.hostname = "127.0.0.1"
akka.actor.deployment = {
  "/host/*" {
		remote = "akka.tcp://RemotingExampleHost@127.0.0.1:40048"
  }
  "/remote/*" {
    remote = "akka.tcp://RemotingExampleRemote@127.0.0.1:1338"
  }
}
""").withFallback(ConfigFactory.load());
    
    implicit val system = ActorSystem("host", remotingConfig)
    system.actorOf(Props(classOf[MyHostActor], List()), name = "ExampleHostActor")
    
  }
}