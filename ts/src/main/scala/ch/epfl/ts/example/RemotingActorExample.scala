package ch.epfl.ts.example

import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef

class MyRemoteActor(hostActor: ActorRef) extends Actor {
  
  hostActor ! "I am alive!"
  
  override def receive = {
    case s: String => {
      println("MyRemoteActor received: " + s)
      hostActor ! "Hi there!"
    }
    case m => println("Remote received weird: " + m)
  }
  
}

object RemotingActorExample {
  def main(args: Array[String]): Unit = {
    
    val remotingConfig = ConfigFactory.parseString(
"""
akka.actor.provider = "akka.remote.RemoteActorRefProvider"
akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
akka.remote.netty.tcp.hostname = "127.0.0.1"
akka.remote.netty.tcp.port = 40001
""").withFallback(ConfigFactory.load());
    
    implicit val system = ActorSystem("remote", remotingConfig)
    
  }
}