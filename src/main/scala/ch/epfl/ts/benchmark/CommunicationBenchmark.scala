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
import java.io.ObjectOutputStream
import java.io.ObjectInputStream

object CommunicationBenchmark {

  val msgQuantity = 1000000

  def main(args: Array[String]) {

    /**
     * Tuple2
     */

    /**
     * Java sockets
     */
//    javaSockets2(msgQuantity)

    /**
     *  akka actors
     */
//    actorsTuple2(msgQuantity)

    /**
     * Tuple3
     */

    /**
     * Java sockets
     */
//    javaSockets3(msgQuantity)

    /**
     *  akka actors
     */
    actorsTuple3(msgQuantity)

  }

  def generateTuple2(quantity: Int): List[Tuple2[Int, Int]] = {
    println("#####----- Tuples of size 2 -----#####")
    var elemsList: List[Tuple2[Int, Int]] = List()
    for (i <- 1 to quantity) {
      elemsList = new Tuple2(i, i) :: elemsList
    }
    elemsList = (-1, -1) :: elemsList
    elemsList = elemsList.reverse
    println("generated list of " + elemsList.size + " tuples.")
    elemsList
  }

  def generateTuple3(quantity: Int): List[Tuple3[Int, Int, Int]] = {
    println("#####----- Tuples of size 3 -----#####")
    var elemsList3: List[Tuple3[Int, Int, Int]] = List()
    for (i <- 1 to quantity) {
      elemsList3 = new Tuple3(i, i, i) :: elemsList3
    }
    elemsList3 = (-1, -1, -1) :: elemsList3
    elemsList3 = elemsList3.reverse
    println("generated list of " + elemsList3.size + " tuples.")
    elemsList3
  }

  def javaSockets2(quantity: Int) = {
    val elemsList = generateTuple2(quantity)
    println("###--- Java Sockets ---###")
    val server = new javaServer2(8765)
    val client = new Socket()
    server.start()
    client.connect(server.server.getLocalSocketAddress)
    val stream = new ObjectOutputStream(client.getOutputStream)
    server.startTime = System.currentTimeMillis();
    elemsList.map(a => { stream.writeObject(a) })
    stream.close()
  }

  def javaSockets3(quantity: Int) = {
    val elemsList3 = generateTuple3(quantity)
    println("###--- Java Sockets ---###")
    val server = new javaServer3(8765)
    val client = new Socket()
    server.start()
    client.connect(server.server.getLocalSocketAddress)
    val stream = new ObjectOutputStream(client.getOutputStream)
    server.startTime = System.currentTimeMillis();
    elemsList3.map(a => { stream.writeObject(a) })
    stream.close()
  }

  def actorsTuple2(quantity: Int) = {
    val elemsList = generateTuple2(quantity)
    println("###--- Akka actors ---###")

    val system = ActorSystem("CommBenchmark")
    val receiver = system.actorOf(Props(new ReceiverActor(msgQuantity)), "receiver")
    val sender = system.actorOf(Props(new SenderActor(receiver)), "sender")
    sender ! StartTuples(elemsList)
  }

  def actorsTuple3(quantity: Int) = {
    val elemsList = generateTuple3(quantity)
    println("###--- Akka actors ---###")

    val system3 = ActorSystem("CommBenchmark3")
    val receiver3 = system3.actorOf(Props(new ReceiverActor3(msgQuantity)), "receiver3")
    val sender3 = system3.actorOf(Props(new SenderActor3(receiver3)), "sender3")
    sender3 ! StartTuples3(elemsList)
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
      receiver ! "Stop"
    }

    case endTime: Long => {
      println("akka time: " + (endTime - startTime) + " ms.")
      context.system.shutdown()
    }
  }
}

class ReceiverActor3(quantity: Int) extends Actor {
  def receive = {
    case Tuple3(a, b, c) => {}
    case "Stop"          => sender ! System.currentTimeMillis()

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
      receiver ! "Stop"
    }

    case endTime: Long => {
      println("akka time: " + (endTime - startTime) + " ms.")
      context.system.shutdown()
    }
  }
}

class ReceiverActor(quantity: Int) extends Actor {
  def receive = {
    case Tuple2(a, b) => {
    }
    case "Stop" => sender ! System.currentTimeMillis()
  }
}

/**
 *
 * Java Server
 *
 */

class javaServer2(port: Int) extends Thread {

  val server = new ServerSocket(port)
  var isRunning = true
  var startTime: Long = 0

  override def run() {

    val worker = server.accept()
    val ois = new ObjectInputStream(worker.getInputStream)
    var newObject: Any = null
    while ({ newObject = ois.readObject(); (newObject != (-1, -1)) }) {
      //      println("Java server: received: " + newObject)
    }
    println("javaTime: " + (System.currentTimeMillis() - startTime) + "ms")
    this.stop()
  }
}

class javaServer3(port: Int) extends Thread {

  val server = new ServerSocket(port)
  var isRunning = true
  var startTime: Long = 0

  override def run() {

    val worker = server.accept()
    val ois = new ObjectInputStream(worker.getInputStream)
    var newObject: Any = null
    while ({ newObject = ois.readObject(); (newObject != (-1, -1, -1)) }) {
      //      println("Java server: received: " + newObject)
    }
    println("javaTime: " + (System.currentTimeMillis() - startTime) + "ms")
    this.stop()
  }
}


