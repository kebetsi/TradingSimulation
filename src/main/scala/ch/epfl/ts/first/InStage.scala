package ch.epfl.ts.first

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import ch.epfl.ts.benchmark.{Stop, Start}
import ch.epfl.ts.data.StreamObject
import scala.reflect.ClassTag

trait Stage {
  def broadcast[T](mesg: T)
}

class InStage[T <: StreamObject: ClassTag](as: ActorSystem, out: List[ActorRef]) {

  val clazz = implicitly[ClassTag[T]].runtimeClass

  // Persistance
  var persistance: Option[Persistance[T]] = None

  // Fetcher
  var fetcherCreator: Option[List[ActorRef] => ActorRef] = None
  var fetcherInterface: Option[Fetch[T]] = None

  // Replay
  var replayOptions: Option[ReplayConfig] = None


  /* Creating persistance actors */
  def withPersistance(p: Persistance[T]): InStage[T]= {
    persistance = Option(p)
    this
  }
  def withPersistance(): InStage[T] = {
    // TODO: Auto-select
    throw new Error("Persistance instance autoselect not implemented")
    this
  }

  /* Creating fetching Actors */
  def withFetcherActor(arC: List[ActorRef] => ActorRef): InStage[T] = {
    fetcherCreator = Option(arC)
    this
  }
  def withFetchInterface(fInter: Fetch[T]): InStage[T] = {
    fetcherInterface = Option(fInter)
    this
  }

  def withReplay(initTime: Long, compression: Double) = {
    replayOptions = Option(ReplayConfig(initTime, compression))
    this
  }

  /* Helper function to create the actors */
  private def createReplayActor(c: ReplayConfig) =
    as.actorOf(Props(classOf[Replay[T]], persistance.get, out, ReplayConfig(c.initTime, c.compression)))

  private def createPersistanceActor()  = as.actorOf(Props(new PersistanceActor[T](persistance.get)))

  private def createFetchActor(dest: List[ActorRef]) = {
    fetcherInterface.get match {
      case e: PushFetch[T] => as.actorOf(Props(classOf[PullFetchActor[T]], e, dest))
      case e: PullFetch[T] => as.actorOf(Props(classOf[PullFetchActor[T]], e, dest))
    }
  }

  /* Initializes the whole */
  def start: ActorRef = {
    var fA, pA, rA, dA: Option[ActorRef] = None
    // We are supposed to be in replay mode
    if (persistance != None && replayOptions != None) {
      rA = replayOptions match {
        case r: Some[ReplayConfig] => Option(createReplayActor(r.get))
      }
    } // Supposed to be in live mode
    else if (fetcherCreator != None || fetcherInterface != None) {
      if (persistance != None){
        pA = Option(createPersistanceActor())
        fA = Option(createFetchActor(pA.get :: out))
      } else {
        fA = Option(createFetchActor(out))
      }
    } else {
      throw new RuntimeException("No fetcher or persistance specified")
    }
    as.actorOf(Props(classOf[InStageMaster], this, fA, pA, rA, dA)) // what the fuck?! (this)
  }

  class InStageMaster(f: Option[ActorRef], p: Option[ActorRef], r: Option[ActorRef], d: Option[ActorRef])
    extends Actor with Stage {
    override def receive = {
      case t:Stop => broadcast[Stop](t)
      case t:Start => broadcast[Start](t)
    }
    override def broadcast[T](msg: T) = {
      f match { case a: Some[ActorRef] => a.get ! msg case None => }
      p match { case a: Some[ActorRef] => a.get ! msg case None => }
      r match { case a: Some[ActorRef] => a.get ! msg case None => }
      d match { case a: Some[ActorRef] => a.get ! msg case None => }
    }
  }
}