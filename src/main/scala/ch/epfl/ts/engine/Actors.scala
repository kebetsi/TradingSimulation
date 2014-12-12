package ch.epfl.ts.engine

import akka.actor.ActorRef

object Actors {
  type WalletManager = ActorRef
  type MatcherEngine = ActorRef
  type Client = ActorRef
}