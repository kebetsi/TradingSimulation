package ch.epfl.ts.remoting

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

case object WorkerIsLive

/**
 * Responsible for overseeing actors instantiated at worker nodes
 */
class MasterActor extends Actor {
  
  var workers: MutableList[ActorRef] = MutableList()
  var nAlive = 0
  
  override def receive = {
    case w: ActorRef => {
      workers += w
    }
    case WorkerIsLive => {
      nAlive += 1
      println("Master sees " + nAlive + " workers available.")
    }
    case s: String => {
      println("MasterActor received: " + s)
    }
    case m => println("MasterActor received weird: " + m)
  }
  
  
  def pingAllWorkers = workers.foreach {
    w => w ! 'Ping
  }
}

class WorkerActor(hostActor: ActorRef, dummyParam: Int) extends Actor {
  
  println("Worker actor with param " + dummyParam + " now running.")
  hostActor ! WorkerIsLive
  
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
        "ts-1-021qv44y.cloudapp.net",
        "ts-2.cloudapp.net",
        "ts-3.cloudapp.net",
        "ts-4.cloudapp.net"
        // TODO: other hosts
    )
    val workerPort = 3333
    val workerSystemName = "remote"
    
    implicit val system = ActorSystem("host", remotingConfig)
    val master = system.actorOf(Props(classOf[MasterActor]), name = "MasterActor")
    
    val allParameterValues = (1 to 20)
    val slicedParameters = {
      val n = (allParameterValues.length / availableWorkers.length).ceil.toInt 
      (0 until availableWorkers.length).map(i => allParameterValues.slice(n * i, (n * i) + n))
    }
    
    // Programmatic deployment: master asks workers to instantiate actors
    val remoteActors = for {
      (workerHost, idx) <- availableWorkers.zipWithIndex
    } yield {
      val address = Address("akka.tcp", workerSystemName, workerHost, workerPort)
      val deploy = Deploy(scope = RemoteScope(address))
      
      // TODO: send different actors to different systems (use built-in load balancing mechanisms?)
      for {
        dummyParam <- slicedParameters(idx)
      } yield {
        val name = "Worker-" + idx + "-" + dummyParam
        val remoteActor = system.actorOf(Props(classOf[WorkerActor], master, dummyParam).withDeploy(deploy), name)
        // Register this new actor to the master
        master ! remoteActor
        
        println("Created " + name + " at host " + workerHost)
      }
    }
  }
}