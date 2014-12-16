package ch.epfl.ts.data


case class Tweet(timestamp: Long, content: String, sentiment: Int, imagesrc: String, author: String) extends StreamObject