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
object HistDataFetcherExample {
  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("HistFetcherExample")

    // variables for the fetcher
    val dateFormat = new java.text.SimpleDateFormat("yyyyMM")
    val startDate = dateFormat.parse("201304");
    val endDate   = dateFormat.parse("201305");
    val workingDir = "./data";
    val currencyPair = "EURCHF";
    
    // Create Components
    // build fetcher
    val fetcher = builder.createRef(Props(classOf[HistDataCSVFetcher], workingDir, currencyPair, startDate, endDate, 60.0),"HistFetcher")    
    // build printer
    val printer = builder.createRef(Props(classOf[Printer], "Printer"), "Printer")

    // Create the connection
    fetcher->(printer, classOf[Quote])

    // Start the system
    builder.start
  }
}