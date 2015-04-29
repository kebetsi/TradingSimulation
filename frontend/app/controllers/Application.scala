package controllers

import actors.TrueFxWsDemoActor
import play.api._
import play.api.Play.current
import play.api.libs.iteratee.Iteratee
import play.api.mvc._
import play.api.libs.json.JsValue
import akka.actor.Props
import actors.TsMsgToJson
import scala.reflect.ClassTag
import ch.epfl.ts.data.OHLC
import ch.epfl.ts.indicators.SMA

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("hello"))
  }

  def ohlc = WebSocket.acceptWithActor[String, String] { request =>
    out => 
      Props(classOf[TsMsgToJson[OHLC]], out, implicitly[ClassTag[OHLC]])
  }

  def sma = WebSocket.acceptWithActor[String, String] { request =>
    out => 
      Props(classOf[TsMsgToJson[SMA]], out, implicitly[ClassTag[SMA]])
  }

  /**
   * Simple demo of a WebSocket that sends TrueFX quotes as JSON to the client
   */
  def wsTest = WebSocket.acceptWithActor[String, JsValue] { request =>
    out => Props(classOf[TrueFxWsDemoActor], out)
  }

}