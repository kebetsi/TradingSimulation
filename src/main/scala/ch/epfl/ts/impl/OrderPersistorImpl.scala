package ch.epfl.ts.impl

import scala.slick.driver.SQLiteDriver
import scala.slick.driver.SQLiteDriver.Table
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.lifted.TableQuery
import scala.slick.lifted.Tag

import ch.epfl.ts.data.Order
import ch.epfl.ts.first.Persistance

class OrderPersistorImpl extends Persistance[Order] {

  def init() = {
    val db = Database.forURL("jdbc:sqlite:testDB.txt", driver = "org.sqlite.JDBC")
  }

//  // Definition of the SUPPLIERS table
//  class Suppliers(tag: Tag) extends Table[(Int, String, String, String, String, String)](tag, "SUPPLIERS") {
//    def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
//    def name = column[String]("SUP_NAME")
//    def street = column[String]("STREET")
//    def city = column[String]("CITY")
//    def state = column[String]("STATE")
//    def zip = column[String]("ZIP")
//
//    // Every table needs a * projection with the same type as the table's type parameter
//    def * = (id, name, street, city, state, zip)
//  }
//  val suppliers = TableQuery[Suppliers]

  type Person = (Int, String, Int, Int)
  class People(tag: Tag) extends Table[Person](tag, "PERSON") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc, O.Default(1))
    def name = column[String]("NAME")
    def age = column[Int]("AGE")
    def addressId = column[Int]("ADDRESS_ID")
    def * = (id, name, age, addressId)
    def address = foreignKey("ADDRESS", addressId, addresses)(_.id)
  }
  lazy val people = TableQuery[People]

  type Address = (Int, String, String)
  class Addresses(tag: Tag) extends Table[Address](tag, "ADDRESS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def street = column[String]("STREET")
    def city = column[String]("CITY")
    def * = (id, street, city)
  }
  lazy val addresses = TableQuery[Addresses]

  def save(t: Order) = {

  }
  def save(ts: List[Order]) = {

  }

  def loadSingle(id: Int): Order = {

    return Order(0.0)
  }

  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    return List()
  }

}