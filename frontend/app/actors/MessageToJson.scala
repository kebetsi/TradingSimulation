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
import scala.reflect.ClassTag
import net.liftweb.json._
import net.liftweb.json.Serialization.write
import com.typesafe.config.ConfigFactory

/**
 * Receives Messages of a given Class Tag from the Trading Simulation backend (ts)
 * and converts them to JSON in order to be passed to the client through a web socket
 *
 * Note: we are using lift-json since there is no easy way to use Play's json
 * library with generic type parameters.
 */
class MessageToJson[T <: AnyRef: ClassTag](out: ActorRef) extends Actor {
  val clazz = implicitly[ClassTag[T]].runtimeClass
  implicit val formats = DefaultFormats

  val config = ConfigFactory.load()
  val name = config.getString("akka.backend.systemName")
  val hostname = config.getString("akka.backend.hostname")
  val port = config.getString("akka.backend.port")
  val actors = context.actorSelection("akka.tcp://" + name + "@" + hostname + ":" + port + "/user/*")

  actors ! ComponentRegistration(self, clazz, "frontend" + clazz)

  def receive() = {
    case msg: T =>
      out ! write(msg)
    case _ =>
  }

}

