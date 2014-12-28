package ch.epfl.ts.impl

import ch.epfl.ts.data.Tweet
import ch.epfl.ts.first.Persistance

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.{Column, TableQuery, Tag}


class TweetPersistorImpl extends Persistance[Tweet] {

  val db = Database.forURL("jdbc:sqlite:testDB.txt", driver = "org.sqlite.JDBC")

  type TweetEntry = (Int, Long, String, Int, String, String)
  class Tweets(tag: Tag) extends Table[(Int, Long, String, Int, String, String)](tag, "TWEETS") {
    def id: Column[Int] = column[Int]("TWEET_ID", O.PrimaryKey, O.AutoInc)
    def timestamp: Column[Long] = column[Long]("TIMESTAMP")
    def content: Column[String] = column[String]("CONTENT")
    def sentiment: Column[Int] = column[Int]("SENTIMENT")
    def imagesrc: Column[String] = column[String]("IMAGE_SRC")
    def author: Column[String] = column[String]("AUTHOR")
    def * = (id, timestamp, content, sentiment, imagesrc, author)
  }
  lazy val tweet = TableQuery[Tweets]

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
      tweet += (1, newTweet.timestamp, newTweet.content, newTweet.sentiment, newTweet.imagesrc, newTweet.author) // AutoInc are implicitly ignored
    }
  }

  /**
   * save entries
   */
  def save(ts: List[Tweet]) = {
    db.withDynSession {
      tweet ++= ts.toIterable.map { x => (1, x.timestamp, x.content, x.sentiment, x.imagesrc, x.author) }
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
   * load entries with timestamp value between startTime and endTime
   */
  def loadBatch(startTime: Long, endTime: Long): List[Tweet] = {
    var res: List[Tweet] = List()
    db.withDynSession {
      val r = tweet.filter(e => e.timestamp >= startTime && e.timestamp <= endTime).invoker.foreach { r => res = new Tweet(r._2, r._3, r._4, r._5, r._6) :: res }
    }
    res
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
   * delete entries with timestamp values between startTime and endTime
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