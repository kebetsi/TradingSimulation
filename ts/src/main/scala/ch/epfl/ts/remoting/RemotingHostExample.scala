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

/**
 * Responsible for overseeing actors instantiated at worker nodes
 */
class MasterActor extends Actor {
  
  var workers: MutableList[ActorRef] = MutableList()
  
  override def receive = {
    case w: ActorRef => {
      workers += w
      pingAllWorkers
    }
    case s: String => {
      println("MasterActor received: " + s)
    }
    case m => println("MasterActor received weird: " + m)
  }
  
  
  def pingAllWorkers = workers.foreach {
    w => w ! "Ping!"
  }
}

class WorkerActor(hostActor: ActorRef, dummyParam: Int) extends Actor {
  
  hostActor ! "I am alive! I have parameter value: " + dummyParam
  
  override def receive = {
    case s: String => {
      println("WorkerActor received: " + s)
      hostActor ! "Hi there!"
    }
    case m => println("WorkerActor received weird: " + m)
  }
  
}

object RemotingHostExample {
  def main(args: Array[String]): Unit = {
    
    // `akka.remote.netty.tcp.hostname` is specified on a per-machine basis in the `application.conf` file
    val remotingConfig = ConfigFactory.parseString(
"""
akka.actor.provider = "akka.remote.RemoteActorRefProvider"
akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
akka.remote.netty.tcp.port = 40000
akka.actor.serialize-creators = on
""").withFallback(ConfigFactory.load());
    
    val availableWorkers = List(
        "ts-1-021qv44y.cloudapp.net"
    )
    val workerPort = 3333
    val workerSystemName = "remote"
    
    implicit val system = ActorSystem("host", remotingConfig)
    val master = system.actorOf(Props(classOf[MasterActor]), name = "MasterActor")
    
    // Programmatic deployment: master asks workers to instantiate actors
    val remoteActors = for {
      worker <- availableWorkers
    } yield {
      val address = Address("akka.tcp", workerSystemName, availableWorkers(0), workerPort)
      val deploy = Deploy(scope = RemoteScope(address))
      
      // TODO: send different actors to different systems (use built-in load balancing mechanisms?)
      for {
        dummyParam <- 1 to 10
      } yield {
        val name = "Worker" + dummyParam
        val remoteActor = system.actorOf(Props(classOf[WorkerActor], master, dummyParam).withDeploy(deploy), name)
        // Register this new actor to the master
        master ! remoteActor
      }
    }
  }
}