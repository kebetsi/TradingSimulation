package ch.epfl.ts.example

import akka.actor.Props
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.fetch.{TrueFxFetcher, PullFetchComponent}
import ch.epfl.ts.component.utils.Printer
import ch.epfl.ts.data.Quote
import scala.reflect.ClassTag

/**
 * @author dmeier
 */

/**
 * This system should instantiate a trueFxFetcher and 
 * display the fetched data live on the command line
 */
object trueFxFetcherShowoff {
  def main(args: Array[String]) {
    implicit val builder = new ComponentBuilder("trueFxShowcase")

    // Instantiate the fetcher
    val trueFxFetcher = new TrueFxFetcher();
    
    // Create Components
    // build fetcher
    val builtFetcher = builder.createRef(
        Props(
            classOf[PullFetchComponent[Quote]],
            trueFxFetcher,
            implicitly[ClassTag[Quote]]
       ), "trueFxFetcher1")    
    // build printer
    val builtPrinter = builder.createRef(Props(classOf[Printer], "printer1"), "printer1")
     
    // Create the connection
    builtFetcher.addDestination(builtPrinter, classOf[Quote])

    // Start the system
    builder.start
  }
}