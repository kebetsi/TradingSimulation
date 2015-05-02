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
import ch.epfl.ts.engine.MarketRules
import ch.epfl.ts.engine.MarketFXSimulator
import ch.epfl.ts.engine.ForexMarketRules
import ch.epfl.ts.component.fetch.TrueFxFetcher
import scala.reflect.ClassTag
import ch.epfl.ts.component.fetch.PullFetchComponent
import ch.epfl.ts.component.fetch.MarketNames
import ch.epfl.ts.data.Quote
import ch.epfl.ts.component.utils.Printer

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

/**
 * Dummy actor to test Akka remoting
 */
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
  
  val availableWorkers = List(
    "ts-1-021qv44y.cloudapp.net",
    "ts-2.cloudapp.net",
    "ts-3.cloudapp.net",
    "ts-4.cloudapp.net"
    // TODO: other hosts
  )
  val workerPort = 3333
  // TODO: lookup in configuration
  val workerSystemName = "remote"
  
  /**
   * Get a list of props that need to be created identically on
   * all actor systems: 
   */
  val commonProps = {
    // Fetcher
    val fetcherActor = new TrueFxFetcher
    val fetcher = Props(classOf[PullFetchComponent[Quote]], fetcherActor, implicitly[ClassTag[Quote]])
    
    // Market
    val rules = new ForexMarketRules()
    val market = Props(classOf[MarketFXSimulator], MarketNames.FOREX_ID, rules)
    
    // Printer
    val printer = Props(classOf[Printer], "")
    
    Map(
      "Fetcher" -> fetcher,
      "Market"  -> market,
      "Printer" -> printer
    )
  }
  
  /**
   * For the given remote worker, create the common components
   * and an instance of the trading strategy for each parameter value given.
   * 
   * @param master Supervisor actor to register the worker to
   * @param host Hostname of the remote actor system
   * @param prefix Prefix to use in each of this system's actor names
   * @param parameterValues
   */
  // TODO: replace by actual parameters
  // TODO: do not hardcode strategy
  // TODO: connect actors
  def createRemoteActors(master: ActorRef, host: String, prefix: String,
                         parameterValues: Iterable[Int])
                        (implicit system: ActorSystem): Unit = {
    val address = Address("akka.tcp", workerSystemName, host, workerPort)
    val deploy = Deploy(scope = RemoteScope(address))
    
    // Common props
    for {
      (name, props) <- commonProps
    } yield {
      system.actorOf(props.withDeploy(deploy), prefix + name)
      println("Created common prop " + name + " at host " + host)
    }
    
    // One trader for each parameterization
    for {
      dummyParam <- parameterValues
    } yield {
      val name = prefix +  "-Worker-" + dummyParam
      
      // TODO: evaluator as well
      val remoteActor = system.actorOf(Props(classOf[WorkerActor], master, dummyParam).withDeploy(deploy), name)
      
      // Register this new actor to the master
      master ! remoteActor
      
      println("Created trader " + name + " at host " + host)
    }
    
    // TODO: connections
  }
  
  def main(args: Array[String]): Unit = {
    
    // `akka.remote.netty.tcp.hostname` is specified on a per-machine basis in the `application.conf` file
    val remotingConfig = ConfigFactory.parseString(
"""
akka.actor.provider = "akka.remote.RemoteActorRefProvider"
akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
akka.remote.netty.tcp.port = 3333
akka.actor.serialize-creators = on
""").withFallback(ConfigFactory.load());
    
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
    } yield createRemoteActors(master, workerHost, idx.toString(), slicedParameters(idx))
  }
}