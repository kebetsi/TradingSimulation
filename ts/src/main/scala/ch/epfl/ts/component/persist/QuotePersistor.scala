package ch.epfl.ts.component.persist

import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Currency
import ch.epfl.ts.component.fetch.MarketNames
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.{Column, TableQuery, Tag}

/**
 * Provides methods to save or load a set of quotes into an SQLite database
 * 
 * @param dbFilename    The database this persistor works on. The actual file accessed
 *                      will be at data/<dbFilename>.db
 */
class QuotePersistor(dbFilename: String) extends Persistance[Quote]{
  
  // Define the QUOTES table format
  class Quotes(tag: Tag) extends Table[(Int, Long, String, String, Double, Double)](tag, "QUOTES") {
    def * = (id, timestamp, whatC, withC, bid, ask)
    def id: Column[Int] = column[Int]("QUOTE_ID", O.PrimaryKey, O.AutoInc)
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def whatC: Column[String] = column[String]("WHAT_C")
    def withC: Column[String] = column[String]("WITH_C")
    def bid: Column[Double] = column[Double]("BID")
    def ask: Column[Double] = column[Double]("ASK")
  }
  
  // Open DB session and query handler on the QUOTES table
  val db = Database.forURL("jdbc:sqlite:data/" + dbFilename + ".db", driver = "org.sqlite.JDBC")
  lazy val QuotesTable = TableQuery[Quotes]
  db.withDynSession {
    if (MTable.getTables("QUOTES").list.isEmpty) {
      QuotesTable.ddl.create
    }
  }
  
  
  override def loadBatch(startTime: Long, endTime: Long): List[Quote] = db.withDynSession {
    val res = QuotesTable.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).invoker
    res.list.map( r => Quote(MarketNames.FOREX_ID, r._2, Currency.fromString(r._3), Currency.fromString(r._4), r._5, r._6))
  }
  // TODO
  def loadSingle(id: Int): ch.epfl.ts.data.Quote = ???

  override def save(newQuotes: List[Quote]): Unit = db.withDynSession {
    // The first field of the quote (QUOTE_ID) is set to -1 but this will be 
    // ignored and auto incremented by jdbc:sqlite in the actual DB table.
    QuotesTable ++= newQuotes.map(q => (-1, q.timestamp, q.whatC.toString, q.withC.toString, q.bid, q.ask))
  }
  def save(q: ch.epfl.ts.data.Quote): Unit = save(List(q))
}