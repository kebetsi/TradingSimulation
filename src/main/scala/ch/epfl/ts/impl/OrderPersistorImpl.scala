package ch.epfl.ts.impl

import ch.epfl.ts.first.Persistance
import ch.epfl.ts.data.Order
import java.util.ArrayList

//import scala.slick.driver.SQLiteDriver
//import scala.slick.jdbc.StaticQuery
//import scala.slick.jdbc.meta.MTable
//import scala.slick.direct.AnnotationMapper.column

import scala.slick.jdbc.JdbcBackend.Database

import scala.slick.lifted.{Tag, TableQuery, Column}
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.ast.TypedType

class OrderPersistorImpl extends Persistance[Order] {

  def init() = {
    val db = Database.forURL("jdbc:sqlite:testDB.txt", driver = "org.sqlite.JDBC")
  }

  // Definition of the SUPPLIERS table
  class Suppliers(tag: Tag) extends Table[(Int, String, String, String, String, String)](tag, "SUPPLIERS") {
    def id: Column[Int] = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
    def name: Column[String] = column[String]("SUP_NAME")
    def street: Column[String] = column[String]("STREET")
    def city: Column[String] = column[String]("CITY")
    def state: Column[String] = column[String]("STATE")
    def zip: Column[String] = column[String]("ZIP")
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, name, street, city, state, zip)
  }
  val suppliers = TableQuery[Suppliers]
  def process(data: Order): Unit = {

  }

  def save(t: Order) = {

  }
  def save(ts: List[Order]) = {

  }

  def loadSingle(id: Int): Order = {

    return new Order(0.0)
  }

  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    return List()
  }

}