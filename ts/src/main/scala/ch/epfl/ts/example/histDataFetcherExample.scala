package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{HistDataCSVFetcher, PushFetchComponent}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Quote
import scala.reflect.ClassTag

/**
 * This system should instantiate a histDataCSVFetcher and 
 * display the fetched data live on the command line
 */
object histDataFetcherExample {
  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("histFetcherShowcase")

    // variables for the fetcher
    val dateFormat = new java.text.SimpleDateFormat("yyyyMM")
    val startDate = dateFormat.parse("201304");
    val endDate   = dateFormat.parse("201305");
    val workingDir = "/Users/dmeier/data";
    val currencyPair = "EURCHF";
    
    // Create Components
    // build fetcher
    val builtFetcher = builder.createRef(Props(classOf[HistDataCSVFetcher], workingDir, currencyPair, startDate, endDate),"histFetcher1")    
    // build printer
    val builtPrinter = builder.createRef(Props(classOf[Printer], "printer1"), "printer1")

    // Create the connection
    builtFetcher->(builtPrinter, classOf[Quote])

    // Start the system
    builder.start
  }
}