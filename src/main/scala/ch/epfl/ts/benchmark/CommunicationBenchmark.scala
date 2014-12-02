package ch.epfl.ts.benchmark

import java.net.Socket
import java.net.ServerSocket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.SocketAddress
import java.net.InetAddress
import java.io.BufferedOutputStream
import java.io.PrintStream
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object CommunicationBenchmark {

  val msgQuantity = 100000

  def main(args: Array[String]) = {

    // test for various tuples size
    
    /**
     * Tuple2
     */
//    var elemsList: List[Tuple2[Int, Int]] = List()
//    for (i <- 1 to msgQuantity) {
//      elemsList = new Tuple2(i, i) :: elemsList
//    }
//    elemsList = elemsList.reverse
//    println("generated list of " + elemsList.size + " tuples.")
//    println("#####----- Tuple of size 2 -----#####")
//
//    /**
//     * Java sockets
//     */
//    println("###--- Java Sockets ---###")
//
//    val server = new javaServer(8765)
//    val client = new Socket()
//    server.start()
//    client.connect(server.server.getLocalSocketAddress)
//    val stream = new PrintStream(new BufferedOutputStream(client.getOutputStream))
//    val startSockets = System.currentTimeMillis();
//    elemsList.map(a => { stream.println(a._1 + "," + a._2); stream.flush() })
//    server.stop()
//    println("javaTime: " + (System.currentTimeMillis() - startSockets) + "ms")
//
//    /**
//     *  akka actors
//     */
//    println("###--- Akka actors ---###")
//
//    val system = ActorSystem("CommBenchmark")
//    val receiver = system.actorOf(Props(new ReceiverActor(msgQuantity)), "receiver")
//    val sender = system.actorOf(Props(new SenderActor(receiver)), "sender")
//    sender ! StartTuples(elemsList)
    
    
    /**
     * Tuple3
     */
    var elemsList3: List[Tuple3[Int, Int, Int]] = List()
    for (i <- 1 to msgQuantity) {
      elemsList3 = new Tuple3(i, i, i) :: elemsList3
    }
    elemsList3 = elemsList3.reverse
    println("generated list of " + elemsList3.size + " tuples.")
    println("#####----- Tuple of size 3 -----#####")

    /**
     * Java sockets
     */
    println("###--- Java Sockets ---###")

    val server3 = new javaServer(8766)
    val client3 = new Socket()
    server3.start()
    client3.connect(server3.server.getLocalSocketAddress)
    val stream3 = new PrintStream(new BufferedOutputStream(client3.getOutputStream))
    val startSockets3 = System.currentTimeMillis();
    elemsList3.map(a => { stream3.println(a._1 + "," + a._2 + "," + a._3); stream3.flush() })
    server3.stop()
    println("javaTime: " + (System.currentTimeMillis() - startSockets3) + "ms")

    /**
     *  akka actors
     */
    println("###--- Akka actors ---###")

    val system3 = ActorSystem("CommBenchmark3")
    val receiver3 = system3.actorOf(Props(new ReceiverActor3(msgQuantity)), "receiver3")
    val sender3 = system3.actorOf(Props(new SenderActor3(receiver3)), "sender3")
    sender3 ! StartTuples3(elemsList3)

  }

}

/**
 * 
 * 
 * Tuple3
 * 
 * 
 * 
 */

case class StartTuples3(tuples: List[Tuple3[Int, Int, Int]])

class SenderActor3(receiver: ActorRef) extends Actor {
  var startTime: Long = 0
  def receive = {
    case StartTuples3(tuples) => {
      startTime = System.currentTimeMillis()
      tuples.map(x => receiver ! x);
    }

    case "Stop" => {
      println("akka time: " + (System.currentTimeMillis() - startTime) + " ms.")
      context.system.shutdown()
    }
  }
}

class ReceiverActor3(quantity: Int) extends Actor {
  def receive = {
    case a: Tuple3[Int, Int, Int] => {
      //      println("receiver actor: " + a._1 + "," + a._2);
      if (a._1 == quantity) {
        sender ! "Stop"
        context.system.shutdown()
      }
    }
  }
}

/**
 * 
 * 
 * Tuples 2
 * 
 * 
 */

case class StartTuples(tuples: List[Tuple2[Int, Int]])

class SenderActor(receiver: ActorRef) extends Actor {
  var startTime: Long = 0
  def receive = {
    case StartTuples(tuples) => {
      startTime = System.currentTimeMillis()
      tuples.map(x => receiver ! x);
    }

    case "Stop" => {
      println("akka time: " + (System.currentTimeMillis() - startTime) + " ms.")
      context.system.shutdown()
    }
  }
}

class ReceiverActor(quantity: Int) extends Actor {
  def receive = {
    case a: Tuple2[Int, Int] => {
      //      println("receiver actor: " + a._1 + "," + a._2);
      if (a._1 == quantity) {
        sender ! "Stop"
        context.system.shutdown()
      }
    }
  }
}

/**
 * 
 * 
 * Java Server
 * 
 * 
 */

class javaServer(port: Int) extends Thread {

  val server = new ServerSocket(port)
  var isRunning = true

  override def run() {

    val worker = server.accept()
    val reader = new BufferedReader(new InputStreamReader(worker.getInputStream))
    var newLine: String = null
    while ((newLine = reader.readLine()) != null) {
      //      println("Java server: received " + newLine)
    }
  }
}


