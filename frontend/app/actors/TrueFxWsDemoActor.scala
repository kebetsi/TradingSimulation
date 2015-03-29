package actors

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor._
import ch.epfl.ts.component.fetch.TrueFxFetcher
import ch.epfl.ts.data._
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.Currency
import play.api.libs.json._
import utils.EnumJsonUtils

object TrueFxWsDemoActor {
  def props(out: ActorRef) = Props(new TrueFxWsDemoActor(out))
}

/**
 * Simple demo of an actor that periodically sends TrueFX quotes as JSON
 */
class TrueFxWsDemoActor(out: ActorRef) extends Actor {
  implicit val currencyFormat = EnumJsonUtils.enumFormat(Currency)
  implicit val quoteFormat = Json.format[Quote]

  val fetcher: TrueFxFetcher = new TrueFxFetcher()
  val tick = context.system.scheduler.schedule(Duration.Zero, 1000 milliseconds, self, TrueFxTick)

  def receive = {
    case TrueFxTick =>
      out ! Json.obj("quotes" -> fetcher.fetch())
  }
}

case object TrueFxTick
