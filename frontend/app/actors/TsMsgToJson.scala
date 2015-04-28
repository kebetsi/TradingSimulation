package actors

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorPath
import play.libs.Akka
import ch.epfl.ts.data.OHLC
import scala.concurrent.ExecutionContext.Implicits.global
import ch.epfl.ts.component.ComponentRegistration
import play.api.libs.json._


class OhlcDemo(out: ActorRef) extends Actor {
  implicit val ohlcFormat = Json.format[OHLC]

  val actors = context.actorSelection("akka.tcp://simpleFX@127.0.0.1:2552/user/*")
  
  actors ! ComponentRegistration(self, classOf[OHLC], "frontendOhlc")
  
  def receive() = {
    case o: OHLC =>
      println(o)
      out ! Json.obj("Ohlc" -> o)
    case _ =>
  }
}

