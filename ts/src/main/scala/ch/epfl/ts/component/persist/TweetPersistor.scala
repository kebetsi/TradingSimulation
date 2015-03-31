package ch.epfl.ts.component.persist

import ch.epfl.ts.data.Tweet
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.{Column, TableQuery, Tag}
import scala.collection.mutable.ListBuffer

/**
 * Implementation of the Persistance for Tweets storage
 */
class TweetPersistor(dbFilename: String) extends Persistance[Tweet] {

  class Tweets(tag: Tag) extends Table[(Int, Long, String, Int, String, String)](tag, "TWEETS") {
    def * = (id, timestamp, content, sentiment, imagesrc, author)
    def id: Column[Int] = column[Int]("TWEET_ID", O.PrimaryKey, O.AutoInc)
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def content: Column[String] = column[String]("CONTENT")
    def sentiment: Column[Int] = column[Int]("SENTIMENT")
    def imagesrc: Column[String] = column[String]("IMAGE_SRC")
    def author: Column[String] = column[String]("AUTHOR")
  }

  type TweetEntry = (Int, Long, String, Int, String, String)
  lazy val tweet = TableQuery[Tweets]
  val db = Database.forURL("jdbc:sqlite:" + dbFilename + ".db", driver = "org.sqlite.JDBC")

  /**
   * create table if it does not exist
   */
  def init() = {
    db.withDynSession {
      if (MTable.getTables("TWEETS").list.isEmpty) {
        tweet.ddl.create
      }
    }
  }

  /**
   * save single entry
   */
  def save(newTweet: Tweet) = {
    db.withDynSession {
      tweet +=(1, newTweet.timestamp, newTweet.content, newTweet.sentiment, newTweet.imagesrc, newTweet.author) // AutoInc are implicitly ignored
    }
  }

  /**
   * save entries
   */
  def save(ts: List[Tweet]) = {
    db.withDynSession {
      tweet ++= ts.toIterable.map { x => (1, x.timestamp, x.content, x.sentiment, x.imagesrc, x.author)}
    }
  }

  /**
   * load entry with id
   */
  def loadSingle(id: Int): Tweet /*Option[Order]*/ = {
    db.withDynSession {
      val r = tweet.filter(_.id === id).invoker.firstOption.get
      return Tweet(r._2, r._3, r._4, r._5, r._6)
    }
  }

  /**
   * load entries with timestamp value between startTime and endTime (inclusive)
   */
  def loadBatch(startTime: Long, endTime: Long): List[Tweet] = {
    var res: ListBuffer[Tweet] = ListBuffer[Tweet]()
    db.withDynSession {
      val r = tweet.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).invoker.foreach { r => res.append(Tweet(r._2, r._3, r._4, r._5, r._6)) }
    }
    res.toList
  }

  /**
   * delete entry with id
   */
  def deleteSingle(id: Int) = {
    db.withDynSession {
      tweet.filter(_.id === id).delete
    }
  }

  /**
   * delete entries with timestamp values between startTime and endTime (inclusive)
   */
  def deleteBatch(startTime: Long, endTime: Long) = {
    db.withDynSession {
      tweet.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).delete
    }
  }

  /**
   * delete all entries
   */
  def clearAll = {
    db.withDynSession {
      tweet.delete
    }
  }
}