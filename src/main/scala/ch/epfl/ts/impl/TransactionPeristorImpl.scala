package ch.epfl.ts.impl

import ch.epfl.ts.first.Persistance
import ch.epfl.ts.data.Transaction

import java.util.ArrayList

import ch.epfl.ts.data.OrderType
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.OrderType._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.lifted.{ Tag, TableQuery, Column }
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.ast.TypedType
import scala.slick.jdbc.meta
import scala.slick.jdbc.meta.MTable

class TransactionPersistorImpl extends Persistance[Transaction] {

  val db = Database.forURL("jdbc:sqlite:testDB.txt", driver = "org.sqlite.JDBC")

  type TransactionEntry = (Int, Double, Double, Long, String, String, String)
  class Transactions(tag: Tag) extends Table[(Int, Double, Double, Long, String, String, String)](tag, "TRANSACTIONS") {
    def id: Column[Int] = column[Int]("TRANSACTION_ID", O.PrimaryKey, O.AutoInc)
    def price: Column[Double] = column[Double]("PRICE")
    def quantity: Column[Double] = column[Double]("QUANTITY")
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def currency: Column[String] = column[String]("CURRENCY")
    def buyer: Column[String] = column[String]("BUYER")
    def seller: Column[String] = column[String]("SELLER")
    def * = (id, price, quantity, timestamp, currency, buyer, seller)
  }
  lazy val transaction = TableQuery[Transactions]

  /**
   * create table if it does not exist
   */
  def init() = {
    db.withDynSession {
      if (MTable.getTables("TRANSACTIONS").list.isEmpty) {
        transaction.ddl.create
      }
    }
  }

  /**
   * save single entry
   */
  def save(newTransaction: Transaction) = {
    db.withDynSession {
      transaction += (1, newTransaction.price, newTransaction.quantity, newTransaction.timestamp, newTransaction.currency.toString, newTransaction.buyer, newTransaction.seller) // AutoInc are implicitly ignored
    }
  }

  /**
   * save entries
   */
  def save(ts: List[Transaction]) = {
    db.withDynSession {
      transaction ++= ts.toIterable.map { x => (1, x.price, x.quantity, x.timestamp, x.currency.toString, x.buyer, x.seller) }
    }
  }

  /**
   * load entry with id
   */
  def loadSingle(id: Int): Transaction /*Option[Order]*/ = {
    db.withDynSession {
      val r = transaction.filter(_.id === id).invoker.firstOption.get
      return Transaction(r._2, r._3, r._4, Currency.withName(r._5), r._6, r._7)
    }
  }

  /**
   * load entries with timestamp value between startTime and endTime
   */
  def loadBatch(startTime: Long, endTime: Long): List[Transaction] = {
    var res: List[Transaction] = List()
    db.withDynSession {
      val r = transaction.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).invoker.foreach { r => res = new Transaction(r._2, r._3, r._4, Currency.withName(r._5), r._6, r._7) :: res }
    }
    return res
  }
  
  /**
   * delete entry with id
   */
  def deleteSingle(id: Int) = {
    db.withDynSession {
      transaction.filter(_.id === id).delete
    }
  }
  
  /**
   * delete entries with timestamp values between startTime and endTime
   */
  def deleteBatch(startTime: Long, endTime: Long) = {
    db.withDynSession {
      transaction.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).delete
    }
  }
  
  /**
   * delete all entries
   */
  def clearAll = {
    db.withDynSession {
      transaction.delete
    }
  }

}