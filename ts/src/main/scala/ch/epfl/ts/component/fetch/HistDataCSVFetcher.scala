package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Currency
import scala.util.parsing.combinator._
import scala.io.Source
import java.util.Date
import java.util.Calendar

class HistDataCSVFetcher(dataDir: String, currencyPair: String, start: Date, end: Date, interval: Int = 200) extends PullFetch[Quote] {
  val workingDir = dataDir+"/"+currencyPair.toUpperCase()+"/";
  val (whatC, withC) = Currency.pairFromString(currencyPair);
  
  val bidPref = "DAT_NT_"+currencyPair.toUpperCase()+"_T_BID_"
  val askPref = "DAT_NT_"+currencyPair.toUpperCase()+"_T_ASK_"
  
  // all quotes this fetcher read from disk, ready to be fetch()ed
  val allQuotes = monthsBetweenStartAndEnd.flatMap(m => parse(bidPref+m+".csv", askPref+m+".csv"))
  
  var quoteIndex = 0 // index of the next quote to be fetched
                     // incremented whenever a quote has been fetched
  
  def fetch(): List[Quote] = { quoteIndex = quoteIndex+1; interval(); List(allQuotes.apply(quoteIndex-1)) }
  
  def interval(): Int = interval
  
  //TODO
  def loadInPersistor(filename: String) {  }
  
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

object CSVParser extends RegexParsers with java.io.Serializable {
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