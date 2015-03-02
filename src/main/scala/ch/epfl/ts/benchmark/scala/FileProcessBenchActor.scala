package ch.epfl.ts.benchmark.scala

import akka.actor.ActorRef
import ch.epfl.ts.component.Component
import ch.epfl.ts.data.Transaction

object FileProcessBenchActor {
  def main(args: Array[String]) = {
    /*
    val system = ActorSystem("DataSourceSystem")
    val reporter = system.actorOf(Props[Reporter])
    val printer = system.actorOf(Props(classOf[ConsumerActor], reporter))

    val mainActor = new InStage[Transaction](system, List(printer))
      .withFetcherActor(TimedReporterActor.fileFetchActor(system, reporter, "fakeData.csv"))
      .start

    mainActor ! new Start(0)


    implicit val builder = new ComponentBuilder("DataSourceSystem")

    val printer = builder.createRef(Props(classOf[Printer], "my-printer"))
    val persistor = builder.createRef(Props(classOf[TransactionPersistanceComponent], "btce-transaction-db"))
    val fetcher = builder.createRef(Props(classOf[BtceTransactionPullFetcherComponent], "my-fetcher"))

    fetcher.addDestination(printer, classOf[Transaction])
    fetcher.addDestination(persistor, classOf[Transaction])

    builder.start

*/
  }
}



class Consumer(reporter: ActorRef) extends Component {
  var notInit = true
  var startTime: Long = 0
  var count: Long = 0
  override def receiver = {
    case t:Transaction => timeConsume
    case _ =>
  }
  def timeConsume: Unit = {
    if (notInit) {
      notInit = false
      startTime = System.currentTimeMillis()
    }
    count += 1
    //println(count)
    if (count == 999477) {
      val endTime = System.currentTimeMillis()
      reporter ! Report("Consumer", startTime, endTime)
    }
  }
}

