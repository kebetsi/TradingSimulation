package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.persist.TransactionPersistor
import ch.epfl.ts.component.replay.{Replay, ReplayConfig}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Transaction

import scala.reflect.ClassTag

/**
 * Demonstration of fetching Live Bitcoin/USD trading data from BTC-e,
 * saving it to a SQLite Database and printing it on the other side.
 */
object ReplayFlowTesterFromStorage {
  def main(args: Array[String]): Unit = {
    implicit val builder = new ComponentBuilder("DataSourceSystem")

    // Initialize the Interface to DB
    val btceXactPersit = new TransactionPersistor("btce-transaction-db")
    btceXactPersit.init()

    // Configuration object for Replay
    val replayConf = new ReplayConfig(1418737788400L, 0.01)

    // Create Components
    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val replayer = builder.createRef(Props(classOf[Replay[Transaction]], btceXactPersit, replayConf, implicitly[ClassTag[Transaction]]))

    // Create the connections
    replayer.addDestination(printer, classOf[Transaction])

    // Start the system
    builder.start
  }
}
