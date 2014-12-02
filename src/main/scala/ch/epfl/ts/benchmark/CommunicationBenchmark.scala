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

  val msgQuantity = 100

  def main(args: Array[String]) = {

    // test for various tuples size
    var elemsList: List[Tuple2[Int, Int]] = List()
    for (i <- 1 to msgQuantity) {
      elemsList = new Tuple2(i, i) :: elemsList
    }
    elemsList = elemsList.reverse
    println("generated list of " + elemsList.size + " tuples.")

    // java sockets
    println("###--- Java Sockets ---###")

    val server = new javaServer(8765)
    val client = new Socket()
    server.start()
    
    
    client.connect(server.server.getLocalSocketAddress)
    val stream = new PrintStream(new BufferedOutputStream(client.getOutputStream))
    val startSockets = System.currentTimeMillis();
    elemsList.map(a => { stream.println(a._1 + "," + a._2); stream.flush() })
    server.stop()
    println("javaTime: " + (System.currentTimeMillis() - startSockets) + "ms")

    /**
     *  akka actors
     */
    println("###--- Akka actors ---###")

    val system = ActorSystem("CommBenchmark")
    val receiver = system.actorOf(Props(new ReceiverActor(msgQuantity)), "receiver")
    val sender = system.actorOf(Props(new SenderActor(receiver)), "sender")
    sender ! StartTuples(elemsList)

  }

}

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


