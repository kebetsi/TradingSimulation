package controllers

import actors.TrueFxWsDemoActor
import play.api._
import play.api.Play.current
import play.api.libs.iteratee.Iteratee
import play.api.mvc._
import play.api.libs.json.JsValue
import akka.actor.Props
import actors.OhlcDemo

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("hello"))
  }

  def traderList = WebSocket.acceptWithActor[String, JsValue] { request =>
    out => Props(classOf[OhlcDemo], out)
  }

  /**
   * Simple demo of a WebSocket that sends TrueFX quotes as JSON to the client
   */
  def wsTest = WebSocket.acceptWithActor[String, JsValue] { request =>
    out => Props(classOf[TrueFxWsDemoActor], out)
  }

}