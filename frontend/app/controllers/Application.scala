package controllers

import play.api._
import play.api.Play.current
import play.api.libs.iteratee.Iteratee
import play.api.mvc._
import play.api.libs.json.JsValue
import akka.actor.Props
import actors.MessageToJson
import scala.reflect.ClassTag
import ch.epfl.ts.data.OHLC
import ch.epfl.ts.indicators.SMA
import ch.epfl.ts.data.Quote
import ch.epfl.ts.data.Transaction

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("hello"))
  }

  def quote = WebSocket.acceptWithActor[String, String] { request =>
    out => 
      Props(classOf[MessageToJson[Quote]], out, implicitly[ClassTag[Quote]])
  }

  def transaction = WebSocket.acceptWithActor[String, String] { request =>
    out => 
      Props(classOf[MessageToJson[Transaction]], out, implicitly[ClassTag[Transaction]])
  }


}