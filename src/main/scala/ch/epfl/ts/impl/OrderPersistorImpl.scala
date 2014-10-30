package ch.epfl.ts.impl


import ch.epfl.ts.first.Persistance
import ch.epfl.ts.data.Order
import java.util.ArrayList
import ch.epfl.ts.types.Currency._
import ch.epfl.ts.data.OrderType._

import scala.slick.jdbc.JdbcBackend.Database

import scala.slick.lifted.{Tag, TableQuery, Column}
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.ast.TypedType

class OrderPersistorImpl extends Persistance[Order] {

  def init() = {
    val db = Database.forURL("jdbc:sqlite:testDB.txt", driver = "org.sqlite.JDBC")
  }

  type OrderEntry = (Int, Double, Double, Long)
  class Orders(tag: Tag) extends Table[(Int, Double, Double, Long)](tag, "ORDERS") {
    def id: Column[Int] = column[Int]("ORD_ID", O.PrimaryKey, O.AutoInc)
    def price: Column[Double] = column[Double]("PRICE")
    def quantity: Column[Double] = column[Double]("QUANTITY")
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def * = (id, price, quantity, timestamp)
  }
  lazy val order = TableQuery[Orders]
  
  def save(newOrder: Order) = {
//    order += newOrder
//    order.map(o => (o.price, o.quantity, o.timestamp))
//      .insert((0, newOrder.price, newOrder.quantity, newOrder.timestamp))
  }  
  
  def save(ts: List[Order]) = {

  }

  def loadSingle(id: Int): Order = {

    return Order(0.0, 0.0, 0, USD, BID)
  }

  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    return List()
  }

}