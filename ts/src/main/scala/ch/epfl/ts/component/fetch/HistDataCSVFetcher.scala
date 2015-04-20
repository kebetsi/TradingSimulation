package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Currency
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
 * @param dataDir       An absolute directory path. The directory should contain substructures
 *                      of the form <currency pair>/<xyz>.csv, e.g.: 
 *                      EURCHF/DAT_NT_EURCHF_T_ASK_201304.csv,
 *                      EURCHF/DAT_NT_EURCHF_T_BID_201304.csv,
 *                      ...
 *                      EURUSD/DAT_NT_EURUSD_T_BID_201305.csv, etc.
 * @param currencyPair  The currency pair to be read from the data directory, e.g. "eurchf", "USDCHF", etc.
 * @param start         From when to read. This can be any date, but will be reduced to its month. That means
 *                      if start is set to 2013-04-24 14:34 the fetcher will start reading the first quote available
 *                      in the data file for April 2013 (as if start was set to 2013-04-01 00:00).
 * @param end           Until when to read. Behaves analogous to start, i.e. if end is set to 2013-06-24 14:34
 *                      the fetcher will still read and send all data in June 2013, as if end was set to 2013-06-30 24:00                    
 */

class HistDataCSVFetcher(dataDir: String, 
                         currencyPair: String, 
                         start: Date, 
                         end: Date
                        ) 
                         extends PushFetchComponent[Quote] {
  
  val workingDir = dataDir+"/"+currencyPair.toUpperCase()+"/";
  val (whatC, withC) = Currency.pairFromString(currencyPair);
  
  val bidPref = "DAT_NT_"+currencyPair.toUpperCase()+"_T_BID_"
  val askPref = "DAT_NT_"+currencyPair.toUpperCase()+"_T_ASK_"
  
  /**
   * The centerpiece of this class, where we actually load the data.
   * It contains all quotes this fetcher reads from disk, ready to be fetch()ed
   */
  val allQuotes: List[Quote] = monthsBetweenStartAndEnd.flatMap(m => parse(bidPref+m+".csv", askPref+m+".csv")).sortBy { q => q.timestamp }
  
  /**
   * Index of the next quote to be fetched, incremented whenever a quote has been fetched
   */
  var quoteIndex = 0
  
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
      if (quoteIndex < allQuotes.length) {
        //update the iterator variables
        var currQ = allQuotes( quoteIndex )
        var nextQ = allQuotes( List(quoteIndex+1,allQuotes.length).min )
        quoteIndex = quoteIndex + 1;
        
        //send the quote and schedule next call
        callback(currQ)
        timer.schedule(new SendQuotes(), nextQ.timestamp - currQ.timestamp)
      } else {
        timer.cancel();
      }
    }
  }
  
  //TODO
  def loadInPersistor(filename: String) {  }
  
  /**
   * Finds the months this HistDataCSVFetcher object should fetch, given the class 
   * constructor arguments (start: Date) and (end: Date)
   * 
   * @return  A list of months of the form List("201411", "201412", "201501", ...)
   */
  def monthsBetweenStartAndEnd: List[String] = {
    val cal = Calendar.getInstance()
    cal.setTime(start) ; val startYear = cal.get(Calendar.YEAR) ; val startMonth = cal.get(Calendar.MONTH)+1
    cal.setTime(end) ; val endYear = cal.get(Calendar.YEAR) ; val endMonth = cal.get(Calendar.MONTH)+1
    
    List.range(startYear, endYear+1)
    .map( year => (year, List.range(1, 12+1)
    .filter ( month => //find the months between start and end
      (startYear != endYear && 
          ((year > startYear && year < endYear)   || 
           (year == endYear && month <= endMonth) || 
           (year == startYear && month >= startMonth)) ) ||
      (startYear == endYear && month <= endMonth && month >= startMonth) )))
    //create 1 string per month, outputs is e.g. List("201304", "201305", ...)
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
    val bidlines = Source.fromFile(workingDir+bidCSVFilename).getLines
    val asklines = Source.fromFile(workingDir+askCSVFilename).getLines
    bidlines.zip(asklines)
    .map( l => l._1+" "+l._2 ) //combine bid and ask data into one line
    .map( l => CSVParser.parse(CSVParser.csvcombo, l).get )
    // withC and whatC are not available in the CVS  
    // we add them after parsing (they are in the path to the file opened above)
    .map( q => Quote(q.marketId, q.timestamp, whatC, withC, q.bid, q.ask) );
  }
}

/**
 * Parser object used by HistDataCSVFetcher.parse() to convert the CSV to Quotes.
 */
object CSVParser extends RegexParsers with java.io.Serializable {
  
  /**
   * The csvcombo format reads a line of the following form (and converts it to a Quote):
   * (Bid csv: DATE TIME; BIDPRICE; *)+" "+(Ask csv: DATE TIME; ASKPRICE; *)
   * 
   * For example:
   * 20130331 235953;1.216450;0 20130331 235953;1.216570;0
   */
  def csvcombo: Parser[Quote] = (
    datestamp~timestamp~";"~floatingpoint~";0"~datestamp~timestamp~";"~floatingpoint~";0" ^^
    { case d~t~_~bid~_~d2~t2~_~ask~_ => Quote(0, toTime(d, t), Currency.DEF, Currency.DEF, bid.toDouble, ask.toDouble) }
  )
  
  val datestamp: Parser[String] = """[0-9]{8}""".r
  val timestamp: Parser[String] = """[0-9]{6}""".r
  val floatingpoint: Parser[String] = """[0-9]*\.?[0-9]*""".r
  
  def toTime(datestamp: String, timestamp: String): Long = { 
    val stampFormat = new java.text.SimpleDateFormat("yyyyMMddHHmmss")    
    val stamp = stampFormat.parse(datestamp+timestamp)
    stamp.getTime
  }
}