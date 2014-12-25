package ch.epfl.ts.impl

import ch.epfl.ts.data.{Currency, Order, OrderType}
import ch.epfl.ts.first.Persistance

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.{Column, TableQuery, Tag}

class OrderPersistorImpl(dbName: String) extends Persistance[Order] {

  val db = Database.forURL("jdbc:sqlite:" + dbName + ".db", driver = "org.sqlite.JDBC")

  type OrderEntry = (Int, Long, Double, Double, Long, String, String)
  class Orders(tag: Tag) extends Table[(Int, Long, Double, Double, Long, String, String)](tag, "ORDERS") {
    def id: Column[Int] = column[Int]("ORD_ID", O.PrimaryKey, O.AutoInc)
    def externalId: Column[Long] = column[Long]("EXT_ID")
    def price: Column[Double] = column[Double]("PRICE")
    def quantity: Column[Double] = column[Double]("QUANTITY")
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def currency: Column[String] = column[String]("CURRENCY")
    def orderType: Column[String] = column[String]("ORDER_TYPE")
    def * = (id, externalId, price, quantity, timestamp, currency, orderType)
  }
  lazy val order = TableQuery[Orders]

  /**
   * create table if it does not exist
   */
  def init() = {
    db.withDynSession {
      if (MTable.getTables("ORDERS").list.isEmpty) {
        order.ddl.create
      }
    }
  }

  /**
   * save single entry
   */
  def save(newOrder: Order) = {
    db.withDynSession {
      order += (1, newOrder.id, newOrder.price, newOrder.quantity, newOrder.timestamp, newOrder.currency.toString, newOrder.orderType.toString) // AutoInc are implicitly ignored
    }
  }

  /**
   * save entries
   */
  def save(os: List[Order]) = {
    db.withDynSession {
      order ++= os.toIterable.map { x => (1, x.id, x.price, x.quantity, x.timestamp, x.currency.toString, x.orderType.toString) }
    }
  }

  /**
   * load entry with id
   */
  def loadSingle(id: Int): Order /*Option[Order]*/ = {
    db.withDynSession {
      val r = order.filter(_.id === id).invoker.firstOption.get
      return Order(r._2, r._3, r._4, r._5, Currency.withName(r._6), OrderType.withName(r._7))
    }
  }

  /**
   * load entries with timestamp value between startTime and endTime
   */
  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    var res: List[Order] = List()
    db.withDynSession {
      val r = order.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).invoker.foreach { r => res = new Order(r._2, r._3, r._4, r._5, Currency.withName(r._6), OrderType.withName(r._7)) :: res }
    }
    return res
  }
  
  /**
   * delete entry with id
   */
  def deleteSingle(id: Int) = {
    db.withDynSession {
      order.filter(_.id === id).delete
    }
  }
  
  /**
   * delete entries with timestamp values between startTime and endTime
   */
  def deleteBatch(startTime: Long, endTime: Long) = {
    db.withDynSession {
      order.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).delete
    }
  }
  
  /**
   * delete all entries
   */
  def clearAll = {
    db.withDynSession {
      order.delete
    }
  }

}