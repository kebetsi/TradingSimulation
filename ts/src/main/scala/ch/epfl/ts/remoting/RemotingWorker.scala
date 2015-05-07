package ch.epfl.ts.remoting

import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef

object RemotingWorker {
  def main(args: Array[String]): Unit = {

    // `akka.remote.netty.tcp.hostname` is specified on a per-machine basis in the `application.conf` file
    val remotingConfig = ConfigFactory.parseString(
"""
akka.actor.provider = "akka.remote.RemoteActorRefProvider"
akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
akka.remote.netty.tcp.bind-hostname = "0.0.0.0"
akka.remote.netty.tcp.port = 3333
""").withFallback(ConfigFactory.load());

    implicit val system = ActorSystem("remote", remotingConfig)
  }
}
