package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.component.replay.{Replay, ReplayConfig}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Transaction

import scala.reflect.ClassTag

/**
 * Demonstration of loading Bitcoin/USD transactions data from a transactions
 * persistor and printing it.
 */
object ReplayFlowTesterFromStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("ReplayFlowTesterSystem")

    // Initialize the Interface to DB
    val btceXactPersit = new TransactionPersistor("btce-transaction-db")
    btceXactPersit.init()

    // Configuration object for Replay
    val replayConf = new ReplayConfig(1418737788400L, 0.01)

    // Create Components
    val printer = builder.createRef(Props(classOf[Printer], "printer"), "printer")
    val replayer = builder.createRef(Props(classOf[Replay[Transaction]], btceXactPersit, replayConf, implicitly[ClassTag[Transaction]]), "replayer")

    // Create the connections
    replayer->(printer, classOf[Transaction])

    // Start the system
    builder.start
  }
}
