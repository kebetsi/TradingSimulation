package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Currency
import ch.epfl.ts.component.persist.QuotePersistor
import ch.epfl.ts.component.fetch.MarketNames._
import scala.util.parsing.combinator._
import scala.io.Source
import scala.concurrent.duration._
import java.util.Date
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask

/**
 * HistDataCSVFetcher class reads data from csv source and converts it to Quotes.
 * For every Quote it has read from disk, it calls the function callback(q: Quote),
 * simulating the past by waiting for a certain time t after each call. By default
 * t is the original (historical) time difference between the quote that was last sent 
 * and the next quote to be sent.
 * 
 * @param dataDir       A directory path containing data files the fetcher can read. Which files are
 *                      actually read is determined by the start and end arguments (see below).
 *                      The directory should contain substructures of the form <currency pair>/<filename>.csv, e.g.:
 *                      EURCHF/DAT_NT_EURCHF_T_ASK_201304.csv,
 *                      EURCHF/DAT_NT_EURCHF_T_BID_201304.csv,
 *                      ...
 *                      EURUSD/DAT_NT_EURUSD_T_BID_201305.csv, etc.
 *
 *                      <filename> is the same name the files have when freshly downloaded from our source,
 *                      in general they take the form "DAT_NT_<currency pair>_T_<bid/ask>_<month>".
 *
 * @param currencyPair  The currency pair to be read from the data directory, e.g. "eurchf", "USDCHF", etc.
 *
 * @param start         From when to read. This can be any date, but will be reduced to its month. That means
 *                      if start is set to 2013-04-24 14:34 the fetcher will start reading the first quote available
 *                      in the data file for April 2013 (as if start was set to 2013-04-01 00:00).
 *
 * @param end           Until when to read. Behaves analogous to start, i.e. if end is set to 2013-06-24 14:34
 *                      the fetcher will still read and send all data in June 2013, as if end was set to 2013-06-30 24:00 
 *
 * @param speed         The speed at which the fetcher will fetch quotes. Defaults to 1 (which means quotes are replayed
 *                      as they were recorded). Can be set e.g. to 2 (time runs twice as fast) or 60 (one hour of historical
 *                      quotes is sent in one minute), etc. Time steps are dT = int(1/speed * dT_historical), in milliseconds.
 *                      Accordingly: be careful with high speeds, if e.g. speed=1000 the system can't make the difference
 *                      between dT_historical=4200 and dT_historical=4900, they will be both sent after 4ms in the simulation.
 *                      If we go even higher, dT will become < 1 and thus 0, which in theory would mean send all the quotes
 *                      you read at the same time, but in practice would probably mess up things.
 */

class HistDataCSVFetcher(dataDir: String, currencyPair: String, 
                         start: Date, end: Date,
                         speed: Double = 1.0)  extends PushFetchComponent[Quote] {
  
  val workingDir = dataDir + "/" + currencyPair.toUpperCase() + "/";
  val (whatC, withC) = Currency.pairFromString(currencyPair);
  
  val bidPref = "DAT_NT_" + currencyPair.toUpperCase() + "_T_BID_"
  val askPref = "DAT_NT_" + currencyPair.toUpperCase() + "_T_ASK_"
  
  /**
   * The centerpiece of this class, where we actually load the data.
   * It contains all quotes this fetcher reads from disk, ready to be fetch()ed.
   * 
   * However, since allQuotes is an Iterator, it acts lazily. That means it only
   * actually reads from disc when it needs to. The data is not prematurely loaded
   * into memory.
   */
  var allQuotes = Iterator[Quote]()
  loadData()
  
  def loadData() = {
    allQuotes = Iterator[Quote]()
    monthsBetweenStartAndEnd.foreach { 
      m => allQuotes ++= parse(bidPref + m + ".csv", askPref + m + ".csv") 
    }
  }
  
  /**
   * Index of the next month to be fetched, incremented whenever a month has been fetched.
   * And the index for the quotes in that month, updated whenever a quote was sent.
   */
  var currentQuote = allQuotes.next()
  var nextQuote = allQuotes.next()
  
  /**
   * Using java.util.Timer to simulate the timing of the quotes when they were generated originally.
   * Schedules a new version of itself t milliseconds after it has send the current quote,
   * 
   * t = (time when next quote was recorded) - (time when current quote was recorded)
   */
  val timer = new Timer()
  timer.schedule(new SendQuotes, 0)
  class SendQuotes extends java.util.TimerTask {
    
    def run() {
      // Get the current quote and send it
      callback(currentQuote)
  
      if (allQuotes.hasNext) {
        // If there is a next quote, schedule the next call
        currentQuote = nextQuote
        nextQuote = allQuotes.next
        timer.schedule(new SendQuotes(), (1 / speed * (nextQuote.timestamp - currentQuote.timestamp)).toInt)
      } else {
        // Nothing more to do here, boys!
        timer.cancel() 
      }
    }
    
  }
  
  /**
   * Saves the quotes this fetcher has fetched (i.e. quotes that are stored in val allQuotes)
   * in an sqlite database of a given name. The fetcher does nothing else during this time.
   * The data is reloaded (rewinded all the way to the start) at the end of the function.
   * 
   * @param   filename    Name of the DB file the quotes will be saved to, the final file
   *                      will be called <filename>.db
   */
  def loadInPersistor(filename: String) {
    val persistor = new QuotePersistor(filename)
    while(allQuotes.hasNext) {
      val batchSize = 100000
      println("Loading batch of "+batchSize+" records into persistor...")
      var batch = List[Quote]()
      var counter = 0
      while(counter < batchSize && allQuotes.hasNext) {
        batch = allQuotes.next :: batch
        counter = counter + 1
      }
      persistor.save(batch)
    }
    // Reload the data because Iterator.next() is destroyed it in the process
    loadData()
  }
  
  /**
   * Finds the months this HistDataCSVFetcher object should fetch, given the class 
   * constructor arguments (start: Date) and (end: Date)
   * 
   * @return  A list of months of the form List("201411", "201412", "201501", ...)
   */
  def monthsBetweenStartAndEnd: List[String] = {
    val cal = Calendar.getInstance()
    
    cal.setTime(start)
    val startYear = cal.get(Calendar.YEAR)
    val startMonth = cal.get(Calendar.MONTH) + 1
    
    cal.setTime(end)
    val endYear = cal.get(Calendar.YEAR)
    val endMonth = cal.get(Calendar.MONTH) + 1
    
    List.range(startYear, endYear + 1)
      // For each of these years, extract the valid months
      .map(year => {
        def isMonthInsidePeriod(month: Int): Boolean = {
          if (startYear != endYear) {
            (year > startYear  && year < endYear)      || 
            (year == startYear && month >= startMonth) ||
            (year == endYear   && month <= endMonth) 
          }
          else {
            (month >= startMonth) && (month <= endMonth)
          }
        }
        
        val validMonths = List.range(1, 12 + 1).filter(isMonthInsidePeriod)
        (year, validMonths)
      })
      // Create one string per month, outputs is e.g. List("201304", "201305", ...)
      .flatMap(l => l._2.map( l2 => l._1.toString + "%02d".format(l2) ))
  }

  /**
   * Given the parent class variable (workingDir: String), reads
   * two files from that directory containing bid and ask data.
   * 
   * @param   bidCSVFilename    Name of the bidCSV file, e.g. "DAT_NT_EURCHF_T_BID_201304.csv"
   * @param   askCSVFilename    Name of the askCSV file, e.g. "DAT_NT_EURCHF_T_ASK_201304.csv"
   * @return                    An iterator of Quotes contained in those two files
   */
  def parse(bidCSVFilename: String, askCSVFilename: String): Iterator[Quote] = {
    val bidlines = Source.fromFile(workingDir + bidCSVFilename).getLines
    val asklines = Source.fromFile(workingDir + askCSVFilename).getLines
    bidlines.zip(asklines)
      // Combine bid and ask data into one line
      .map(l => {
        val q = CSVParser.parse(CSVParser.csvcombo, l._1 + " " + l._2).get
        // `withC` and `whatC` are not available in the CVS, we add them back
        // after parsing (they are in the path to the file opened above)
        Quote(q.marketId, q.timestamp, whatC, withC, q.bid, q.ask)
      })
  }
  
}

/**
 * Parser object used by HistDataCSVFetcher.parse() to convert the CSV to Quotes.
 */
object CSVParser extends RegexParsers with java.io.Serializable {
  
  /**
   * The csvcombo format reads a line of text that has been stitched together from a bid
   * csv line and a corresponding ask csv line, and converts it to a Quote:
   * 
   * For example:
   * 20130331 235953;1.216450;0 20130331 235953;1.216570;0
   * <-bid------csv------part-> <-ask------csv------part->
   */
  def csvcombo: Parser[Quote] = {
    datestamp~timestamp~";"~floatingpoint~";0"~datestamp~timestamp~";"~floatingpoint~";0" ^^ {
      case d~t~_~bid~_~d2~t2~_~ask~_ => Quote(MarketNames.FOREX_ID, toTime(d, t),
                                              Currency.DEF, Currency.DEF,
                                              bid.toDouble, ask.toDouble) 
    }
  }
  
  val datestamp: Parser[String] = """[0-9]{8}""".r
  val timestamp: Parser[String] = """[0-9]{6}""".r
  val floatingpoint: Parser[String] = """[0-9]*\.?[0-9]*""".r
  
  val stampFormat = new java.text.SimpleDateFormat("yyyyMMddHHmmss")    
  def toTime(datestamp: String, timestamp: String): Long = { 
    stampFormat.parse(datestamp + timestamp).getTime
  }
}