package ch.epfl.ts.component.persist

import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.Currency
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.{Column, TableQuery, Tag}

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
  val db = Database.forURL("jdbc:sqlite:" + dbFilename + ".db", driver = "org.sqlite.JDBC")
  lazy val QuotesTable = TableQuery[Quotes]
  def init() = db.withDynSession { if (MTable.getTables("QUOTES").list.isEmpty) { QuotesTable.ddl.create } }
  
  
  override def loadBatch(startTime: Long, endTime: Long): List[Quote] = db.withDynSession {
    val res = QuotesTable.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).invoker
    res.list.map( r => Quote(1, r._2, Currency.fromString(r._3), Currency.fromString(r._4), r._5, r._6))
  }

  override def save(newQuotes: List[Quote]): Unit = db.withDynSession {
    QuotesTable ++= newQuotes.map(q => (1, q.timestamp, q.whatC.toString, q.withC.toString, q.bid, q.ask))
  }
  
  //TODO
  def loadSingle(id: Int): ch.epfl.ts.data.Quote = { Quote(1,1,Currency.EUR,Currency.CHF,1,1) }
  
  //TODO
  def save(t: ch.epfl.ts.data.Quote): Unit = { }

}