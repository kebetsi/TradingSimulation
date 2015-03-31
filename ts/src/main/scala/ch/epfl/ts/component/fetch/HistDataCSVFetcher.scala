package ch.epfl.ts.component.fetch

import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Currency
import scala.util.parsing.combinator._
import scala.io.Source
import java.util.Date

class HistDataCSVFetcher(dataDir: String, currencyPair: String, start: Date, end: Date) extends PullFetch[Quote] {
  val workingDir = dataDir+"/"+currencyPair.toUpperCase()+"/";
  val (whatC, withC) = Currency.pairFromString(currencyPair);
  
  val bidPref = "DAT_NT_"+currencyPair.toUpperCase()+"_T_BID_"
  val askPref = "DAT_NT_"+currencyPair.toUpperCase()+"_T_ASK_"
  
  def monthsBetweenStartAndEnd: List[String] = {
    List.range(start.getYear, end.getYear)
    .map( year => (year, List.range(1, 12)
    .filter ( month => //find the months between start and end
      (year != start.getYear && year != end.getYear) ||
      (year == start.getYear && month >= start.getMonth) ||
      (year == end.getYear && month <= end.getMonth) )))
    .flatMap(l => l._2.map( l2 => l._1.toString + l2.toString ))
  }
  
  //TODO
  def fetch(): List[Quote] = monthsBetweenStartAndEnd.flatMap(l => parse(bidPref+l+".csv", askPref+l+".csv"))
  
  //TODO
  def loadInPersistor(filename: String) {  }
  
  //TODO: implement a method that returns a dynamic time
  // e.g. ((time of next quote to send) - (time of last quote sent)) * (speedup-factor)
  def interval(): Int = 5000
  
  
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
  
  val datestamp: Parser[String] = """[0-9]{9}""".r
  val timestamp: Parser[String] = """[0-9]{6}""".r
  val floatingpoint: Parser[String] = """[-+]?[0-9]*\.?[0-9]*""".r
  
  def toTime(datestamp: String, timestamp: String): Long = { 
    val stampFormat = new java.text.SimpleDateFormat("yyyyMMddHHmmss")    
    val stamp = stampFormat.parse(datestamp+timestamp)
    stamp.getTime
  }
}