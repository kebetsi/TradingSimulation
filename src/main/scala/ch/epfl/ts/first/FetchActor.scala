package ch.epfl.ts.first

import akka.actor.Actor
import scala.reflect.ClassTag

import ch.epfl.ts.data.{Transaction, Order}


protected[first] class PullFetchActor[T](f: Fetch[T]) extends Actor {
	
  def receive = {
    case Transaction 	=> println("PullFetchActor: Got Transaction")
    case Order 			=> println("PullFetchActor: Got Order")
    case _				=> println("PullFetchActor: Unknown Datatype") 
  }
  
}


protected[first] class PushFetchActor[T](f: Fetch[T]) extends Actor {
	
  def receive = {
    case Transaction 	=> println("PushFetchActor: Got Transaction")
    case Order 			=> println("PushFetchActor: Got Order")
    case _				=> println("PushFetchActor: Unknown Datatype") 
  }
  
}