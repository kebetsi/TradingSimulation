package ch.epfl.ts.example

import scala.collection.mutable.MutableList

import com.typesafe.config.ConfigFactory

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.Deploy
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.remote.RemoteScope

class MyHostActor extends Actor {
  
  var workers: MutableList[ActorRef] = MutableList()
  
  override def receive = {
    case w: ActorRef => {
      workers += w
      pingAllWorkers
    }
    case s: String => {
      println("MyHostActor received: " + s)
    }
    case m => println("Host received weird: " + m)
  }
  
  
  def pingAllWorkers = workers.foreach {
    w => w ! "Ping!"
  }
}

object RemotingHostExample {
  def main(args: Array[String]): Unit = {
    
    val remotingConfig = ConfigFactory.parseString(
"""
akka.actor.provider = "akka.remote.RemoteActorRefProvider"
akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
akka.remote.netty.tcp.hostname = "127.0.0.1"
akka.remote.netty.tcp.port = 40000
akka.actor.serialize-creators = on
""").withFallback(ConfigFactory.load());
        
    implicit val system = ActorSystem("host", remotingConfig)
    val myHost = system.actorOf(Props(classOf[MyHostActor]), name = "ExampleHostActor")
    
    // Config-based deployment
    //akka.actor.deployment = {
    //  "/host/*" {
    //    remote = "akka.tcp://host@127.0.0.1:40000"
    //  }
    //  "/remote/*" {
    //    remote = "akka.tcp://remote@ts-1-021qv44y.cloudapp.net:3333"
    //  }
    //}
    // Programatic deployment
    // Create actor from our host on our remote
    val address = Address("akka.tcp", "remote", "ts-1-021qv44y.cloudapp.net", 3333)
    val deploy = Deploy(scope = RemoteScope(address))
    val remoteActor = system.actorOf(Props(classOf[MyRemoteActor], myHost).withDeploy(deploy), name = "ExampleRemoteActor")
    
    // TODO: bound address != expected address
    
    myHost ! "Hello world"
    myHost ! remoteActor
  }
}