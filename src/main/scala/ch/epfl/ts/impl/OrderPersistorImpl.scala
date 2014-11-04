package ch.epfl.ts.impl

import ch.epfl.ts.first.Persistance
import ch.epfl.ts.data.Order
import java.util.ArrayList
import ch.epfl.ts.types.Currency._
import ch.epfl.ts.data.OrderType._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.lifted.{ Tag, TableQuery, Column }
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.ast.TypedType
import scala.slick.jdbc.meta
import scala.slick.jdbc.meta.MTable

class OrderPersistorImpl extends Persistance[Order] {

  val db = Database.forURL("jdbc:sqlite:testDB.txt", driver = "org.sqlite.JDBC")

  type OrderEntry = (Int, Double, Double, Long)
  class Orders(tag: Tag) extends Table[(Int, Double, Double, Long)](tag, "ORDERS") {
    def id: Column[Int] = column[Int]("ORD_ID", O.PrimaryKey, O.AutoInc)
    def price: Column[Double] = column[Double]("PRICE")
    def quantity: Column[Double] = column[Double]("QUANTITY")
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def * = (id, price, quantity, timestamp)
  }
  lazy val order = TableQuery[Orders]

  def init() = {
    db.withDynSession {
      if (MTable.getTables("ORDERS").list.isEmpty) {
        (order.ddl).create
      }
    }
  }

  def save(newOrder: Order) = {
    db.withDynSession {
      order += (1, newOrder.price, newOrder.quantity, newOrder.timestamp) // AutoInc are implicitly ignored
    }
    //    session.
    //    order.insert()
    //    order += (newOrder.price, newOrder.quantity, newOrder.timestamp)
    //    order.map(o => (0, o.price, o.quantity, o.timestamp))
    //      .insert((0, newOrder.price, newOrder.quantity, newOrder.timestamp))
    //    order.map( o => (1, o.price, o.quantity, o.timestamp)) += (1, newOrder.price, newOrder.quantity, newOrder.timestamp)
    //    order.map( o => (1, o.price, o.quantity, o.timestamp)).
  }

  def save(ts: List[Order]) = {
    db.withDynSession {
      //      order ++= ts
    }
  }

  def loadSingle(id: Int): Order /*Option[Order]*/ = {
    db.withDynSession {
      val r = order.filter(_.id === id).invoker.firstOption.get
      return Order(r._2, r._3, r._4)
    }
  }

  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    return List()
  }

}