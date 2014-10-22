package ch.epfl.ts.first

import akka.actor.{Actor, ActorRef}

protected[first] trait Fetch[T] {
	
}

abstract class PullFetch[T] extends Fetch[T] {
  def fetch: List[T]
}

abstract class PushFetch[T] () extends Fetch[T] {
  
  
}