package ch.epfl.ts.component.persist

import ch.epfl.ts.data.Currency._
import ch.epfl.ts.data.{Currency, DelOrder, LimitAskOrder, LimitBidOrder, MarketAskOrder, MarketBidOrder, Order}
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.{Column, TableQuery, Tag}
import scala.collection.mutable.ListBuffer

object OrderType extends Enumeration {
  type OrderType = Value
  val LIMIT_BID = Value("LB")
  val LIMIT_ASK = Value("LA")
  val MARKET_BID = Value("MB")
  val MARKET_ASK = Value("MA")
  val DEL = Value("D")
}
import ch.epfl.ts.component.persist.OrderType._


case class PersistorOrder(val oid: Long, val uid: Long, val timestamp: Long, val whatC: Currency, val withC: Currency, val volume: Double, val price: Double, val orderType: OrderType) extends Order

/**
 * Implementation of the Persistance trait for Order
 */
class OrderPersistor(dbFilename: String) extends Persistance[Order] {

  val nullStringValue = "NULL"

  class Orders(tag: Tag) extends Table[(Int, Long, Long, Long, String, String, Double, Double, String)](tag, "ORDERS") {
    def * = (id, oid, uid, timestamp, whatC, withC, volume, price, orderType)
    def id: Column[Int] = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def oid: Column[Long] = column[Long]("ORDER_ID")
    def uid: Column[Long] = column[Long]("USER_ID")
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def whatC: Column[String] = column[String]("WHAT_C")
    def withC: Column[String] = column[String]("WITH_C")
    def volume: Column[Double] = column[Double]("VOLUME")
    def price: Column[Double] = column[Double]("PRICE")
    def orderType: Column[String] = column[String]("ORDER_TYPE")
  }

  type OrderEntry = (Int, Long, Long, Long, String, String, Double, Double, String)
  lazy val order = TableQuery[Orders]
  val db = Database.forURL("jdbc:sqlite:" + dbFilename + ".db", driver = "org.sqlite.JDBC")

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
      newOrder match {
        case la: LimitAskOrder  => order += (1, la.oid, la.uid, la.timestamp, la.whatC.toString, la.withC.toString, la.volume, la.price, LIMIT_ASK.toString)
        case lb: LimitBidOrder  => order += (1, lb.oid, lb.uid, lb.timestamp, lb.whatC.toString, lb.withC.toString, lb.volume, lb.price, LIMIT_BID.toString)
        case mb: MarketBidOrder => order += (1, mb.oid, mb.uid, mb.timestamp, mb.whatC.toString, mb.withC.toString, mb.volume, 0, MARKET_BID.toString)
        case ma: MarketAskOrder => order += (1, ma.oid, ma.uid, ma.timestamp, ma.whatC.toString, ma.withC.toString, ma.volume, 0, MARKET_ASK.toString)
        case del: DelOrder      => order += (1, del.oid, del.uid, del.timestamp, nullStringValue, nullStringValue, 0, 0, DEL.toString)
        case _                  => println(dbFilename + " Persistor: save error")
      }
    }
  }

  /**
   * save entries
   */
  def save(os: List[Order]) = {
    db.withDynSession {
      order ++= os.toIterable.map {
          case la: LimitAskOrder  => (1, la.oid, la.uid, la.timestamp, la.whatC.toString, la.withC.toString, la.volume, la.price, LIMIT_ASK.toString)
          case lb: LimitBidOrder  => (1, lb.oid, lb.uid, lb.timestamp, lb.whatC.toString, lb.withC.toString, lb.volume, lb.price, LIMIT_BID.toString)
          case mb: MarketBidOrder => (1, mb.oid, mb.uid, mb.timestamp, mb.whatC.toString, mb.withC.toString, mb.volume, 0.0, MARKET_BID.toString)
          case ma: MarketAskOrder => (1, ma.oid, ma.uid, ma.timestamp, ma.whatC.toString, ma.withC.toString, ma.volume, 0.0, MARKET_ASK.toString)
          case del: DelOrder      => (1, del.oid, del.uid, del.timestamp, nullStringValue, nullStringValue, 0.0, 0.0, DEL.toString)

      }
    }
  }

  /**
   * load entry with id
   */
  def loadSingle(id: Int): Order /*Option[Order]*/ = {
    db.withDynSession {
      val r = order.filter(_.id === id).invoker.firstOption.get
      OrderType.withName(r._9) match {
        case LIMIT_BID  => return LimitBidOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, r._8)
        case LIMIT_ASK  => return LimitAskOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, r._8)
        case MARKET_BID => return MarketBidOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, 0.0)
        case MARKET_ASK => return MarketAskOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, 0.0)
        case DEL        => return DelOrder(r._2, r._3, r._4, DEF, DEF, 0.0, 0.0)
        case _          => println(dbFilename + " Persistor: loadSingle error"); return null
      }
    }
  }

  /**
   * load entries with timestamp value between startTime and endTime (inclusive)
   */
  def loadBatch(startTime: Long, endTime: Long): List[Order] = {
    var res: ListBuffer[Order] = new ListBuffer[Order]()
    db.withDynSession {
      val r = order.filter(e => (e.timestamp >= startTime) && (e.timestamp <= endTime)).invoker.foreach { r =>
        OrderType.withName(r._9) match {
          case LIMIT_BID  => res.append(LimitBidOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, r._8))
          case LIMIT_ASK  => res.append(LimitAskOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, r._8))
          case MARKET_BID => res.append(MarketBidOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, 0.0))
          case MARKET_ASK => res.append(MarketAskOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, 0.0))
          case DEL        => res.append(DelOrder(r._2, r._3, r._4, DEF, DEF, 0.0, 0.0))
          case _          => println(dbFilename + " Persistor: loadBatch error")
        }
      }
    }
    res.toList
  }
  
  /**
   * loads the amount of entries provided in the function
   * argument at most.
   */
  def loadBatch(count: Int): List[Order] = {
    var res: ListBuffer[Order] = new ListBuffer[Order]()
    db.withDynSession {
      val r = order.filter(e => e.id <= count).invoker.foreach { r =>
        OrderType.withName(r._9) match {
          case LIMIT_BID  => res.append(LimitBidOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, r._8))
          case LIMIT_ASK  => res.append(LimitAskOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, r._8))
          case MARKET_BID => res.append(MarketBidOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, 0.0))
          case MARKET_ASK => res.append(MarketAskOrder(r._2, r._3, r._4, Currency.withName(r._5), Currency.withName(r._6), r._7, 0.0))
          case DEL        => res.append(DelOrder(r._2, r._3, r._4, DEF, DEF, 0.0, 0.0))
          case _          => println(dbFilename + " Persistor: loadBatch error")
        }
      }
    }
    res.toList
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
   * delete entries with timestamp values between startTime and endTime (inclusive)
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