package com.github.dnvriend

import akka.actor.{ ActorSystem, Props }
import akka.persistence.PersistentActor
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object CounterActor {
  sealed trait Command
  final case class Increment(value: Int) extends Command
  final case class Decrement(value: Int) extends Command

  sealed trait Event
  final case class Incremented(value: Int) extends Event
  final case class Decremented(value: Int) extends Event

  case class CounterState(value: Int = 0) {
    def update(event: Event): CounterState = event match {
      case Incremented(incrementBy) ⇒ copy(value + incrementBy)
      case Decremented(decrementBy) ⇒ copy(value - decrementBy)
    }
  }

  final val PersistenceId: String = "COUNTER"
}

class CounterActor(implicit ec: ExecutionContext) extends PersistentActor {
  import CounterActor._

  val persistenceId: String = PersistenceId

  private var state = CounterState()

  import scala.concurrent.duration._

  context.system.scheduler.schedule(1.second, 1.second, self, Increment(1))
  context.system.scheduler.schedule(5.second, 5.second, self, Decrement(1))

  private def handleEvent(event: Event): Unit = {
    state = state.update(event)
    println("==> Current state: " + state)
  }

  override def receiveRecover: Receive = {
    case event: Incremented ⇒ handleEvent(event)
    case event: Decremented ⇒ handleEvent(event)
  }

  override def receiveCommand: Receive = {
    case Increment(value) ⇒
      println(s"==> Incrementing with: $value")
      persist(Incremented(value))(handleEvent)

    case Decrement(value) ⇒
      println(s"==> Decrementing with: $value")
      persist(Decremented(value))(handleEvent)
  }
}

object LaunchCounter extends App {
  val configName = "counter-application.conf"
  lazy val configuration = ConfigFactory.load(configName)
  implicit val system: ActorSystem = ActorSystem("demo", configuration)
  sys.addShutdownHook(system.terminate())
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  val counter = system.actorOf(Props(new CounterActor))

  // async event listener
  //  lazy val readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
  // does not yet work as we have to implements a custom ReadJournalDao :)
  //  readJournal.eventsByPersistenceId(CounterActor.PersistenceId, 0, Long.MaxValue).runForeach {
  //    case e ⇒ println(": >>== Received event ==<< : " + e)
  //  }

  val banner =
    s"""
       |Counter
       |=======
       |#####  ###### #    #  ####
       |#    # #      ##  ## #    #
       |#    # #####  # ## # #    #
       |#    # #      #    # #    #
       |#    # #      #    # #    #
       |#####  ###### #    #  ####
       |
                  |$BuildInfo
       |
  """.stripMargin

  println(banner)
}
