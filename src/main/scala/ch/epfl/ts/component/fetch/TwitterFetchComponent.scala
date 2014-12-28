package ch.epfl.ts.component.fetch

import java.io.{BufferedReader, InputStreamReader}

import ch.epfl.ts.data.Tweet
import twitter4j._


/**
 * TODO: Should be refactored to only use PushFetchComponent[Tweet] and no more PushFetch[Tweet].
 */
class TwitterFetchComponent extends PushFetchComponent[Tweet] {
  new TwitterPushFetcher(this.callback)
}

class TwitterPushFetcher(override var callback: (Tweet => Unit)) extends PushFetch[Tweet]() {

  val config = new twitter4j.conf.ConfigurationBuilder()
    .setOAuthConsumerKey("h7HL6oGtIOrCZN53TbWafg")
    .setOAuthConsumerSecret("irg8l38K4DUrqPV638dIfXvK0UjVHKC936IxbaTmqg")
    .setOAuthAccessToken("77774972-eRxDxN3hPfTYgzdVx99k2ZvFjHnRxqEYykD0nQxib")
    .setOAuthAccessTokenSecret("FjI4STStCRFLjZYhRZWzwTaiQnZ7CZ9Zrm831KUWTNZri")
    .build

  val twitterStream = new TwitterStreamFactory(config).getInstance
  twitterStream.addListener(simpleStatusListener)
  twitterStream.filter(new FilterQuery().track(Array("bitcoin", "cryptocurrency", "btc", "bitcoins")))

  def simpleStatusListener = new StatusListener() {
    override def onStatus(status: Status) {
      if (status.getUser.getFollowersCount < 30) {
        return
      }
      val tweet = status.getText.replace('\n', ' ')

      // send stuff to datasource
      val commands = Array("python", "twitter-classifier/sentiment.py", tweet)
      val p = Runtime.getRuntime.exec(commands)

      val stdInput = new BufferedReader(new InputStreamReader(p.getInputStream))
      val stdError = new BufferedReader(new InputStreamReader(p.getErrorStream))

      val sentiment = stdInput.readLine()

      val intSentiment = sentiment match {
        case "positive" => 1
        case "negative" => -1
        case "neutral" => 0
        case _ => throw new RuntimeException("Undefined sentiment value")
      }

      if (intSentiment == 1) {
        println(tweet)
      } else if (intSentiment == -1) {
        System.err.println(tweet)
      }
      val imagesrc = status.getUser.getProfileImageURL
      val author = status.getUser.getScreenName
      val ts = status.getCreatedAt.getTime

      callback(new Tweet(ts, tweet, intSentiment, imagesrc, author))

    }

    override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}

    override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}

    override def onException(ex: Exception) {
      ex.printStackTrace()
    }

    override def onScrubGeo(arg0: Long, arg1: Long) {}

    override def onStallWarning(warning: StallWarning) {}
  }
}