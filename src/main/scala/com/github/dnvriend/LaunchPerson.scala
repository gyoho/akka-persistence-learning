package com.github.dnvriend

import java.text.SimpleDateFormat
import java.util.{ Date, UUID }

import akka.NotUsed
import akka.actor._
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.event.LoggingReceive
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.scaladsl._
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.dao.{ PersonDao, PersonDaoImpl }
import com.github.dnvriend.data.Event.PBPersonCreated
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Person {

  sealed trait Command

  final case class CreatePerson(firstName: String, lastName: String, timestamp: Long) extends Command

  final case class ChangeFirstName(firstName: String, timestamp: Long) extends Command

  final case class ChangeLastName(lastName: String, timestamp: Long) extends Command

  // events
  sealed trait Event
  final case class PersonCreated(firstName: String, lastName: String, timestamp: Long) extends Event
  final case class FirstNameChanged(firstName: String, timestamp: Long) extends Event
  final case class LastNameChanged(lastName: String, timestamp: Long) extends Event

  // the state
  final case class PersonState(firstName: String = "", lastName: String = "")

  // necessary for cluster sharding
  final case class EntityEnvelope(id: String, payload: Any)

  final val NumberOfShards: Int = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, payload) ⇒ (id.toString, payload)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityEnvelope(id, _) ⇒ (id.hashCode % NumberOfShards).toString
  }

  final val PersonShardName = "Person"
}

class Person extends PersistentActor with ActorLogging {
  import Person._
  import ShardRegion.Passivate

  override val persistenceId: String = "Person-" + self.path.name

  context.setReceiveTimeout(300.millis)

  var state = PersonState()

  def handleEvent(event: Event): Unit = event match {
    case PersonCreated(firstName, lastName, _) ⇒ state = state.copy(firstName = firstName, lastName = lastName)
    case FirstNameChanged(firstName, _)        ⇒ state = state.copy(firstName = firstName)
    case LastNameChanged(lastName, _)          ⇒ state = state.copy(lastName = lastName)
  }

  override def receiveRecover: Receive = LoggingReceive {
    case event: Event ⇒ handleEvent(event)
  }

  def now: Long = System.currentTimeMillis()

  override def receiveCommand: Receive = LoggingReceive {
    case CreatePerson(firstName, lastName, _) ⇒ persist(PersonCreated(firstName, lastName, now))(handleEvent)
    case ChangeFirstName(firstName, _)        ⇒ persist(FirstNameChanged(firstName, now))(handleEvent)
    case ChangeLastName(lastName, _)          ⇒ persist(LastNameChanged(lastName, now))(handleEvent)
    case ReceiveTimeout                       ⇒ context.parent ! Passivate(stopMessage = SupervisorStrategy.Stop)
    case SupervisorStrategy.Stop              ⇒ context.stop(self)
  }
}

object SupportDesk {
  final case class ChangeFirstName(id: String)
  final case class ChangeLastName(id: String)
}

class SupportDesk(personRegion: ActorRef, readJournal: ReadJournal with CurrentPersistenceIdsQuery)(implicit val mat: Materializer, ec: ExecutionContext) extends Actor with ActorLogging {
  import Person._
  private var counter: Int = 0

  context.system.scheduler.schedule(1.second, 1.second, self, "GO")

  def now: Long = System.currentTimeMillis()

  override def receive: Receive = {
    case _ if counter % 2 == 0 ⇒
      val id = UUID.randomUUID.toString
      personRegion ! EntityEnvelope(id, CreatePerson("FOO", "BAR", now))
      context.system.scheduler.scheduleOnce(5.seconds, self, SupportDesk.ChangeFirstName(id))
      context.system.scheduler.scheduleOnce(10.seconds, self, SupportDesk.ChangeLastName(id))
      counter += 1

    case SupportDesk.ChangeFirstName(id) ⇒
      personRegion ! EntityEnvelope(id, ChangeFirstName(s"FOO-${DateUtil.format(now)}", now))

    case SupportDesk.ChangeLastName(id) ⇒
      personRegion ! EntityEnvelope(id, ChangeLastName(s"BAR-${DateUtil.format(now)}", now))

    case _ ⇒
      counter += 1
      println("Nothing to do: " + counter)
  }
}

object DateUtil {
  def format(timestamp: Long): String =
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.XXX").format(new Date(timestamp))
}

object InsertPersonInPersonTableHandler {
  sealed trait Event
  final case class PersonHandled(offset: Long) extends Event
  final case class PersonInserted(id: String) extends Event
  final case class Completed()
  final case class Ack()
  final case class Init()

  final case class SavePersonSucceeded(offset: Long, sender: ActorRef)
}

/**
 * Handles only PersonCreated events to insert a record in the person.persons table (read model)
 */
class InsertPersonInPersonTableHandler(readJournal: JdbcReadJournal, personDao: PersonDao)(implicit ec: ExecutionContext, mat: Materializer) extends PersistentActor {
  import InsertPersonInPersonTableHandler._
  override def persistenceId: String = "InsertPersonInPersonTableHandler"

  var recoverOffsetPersonCreated: Long = 0

  def handleEvent(event: Event): Unit = event match {
    case PersonHandled(newOffset) ⇒ recoverOffsetPersonCreated = newOffset
    case _                        ⇒
  }

  override def receiveRecover: Receive = LoggingReceive {
    case event: Event ⇒ handleEvent(event)
    case RecoveryCompleted ⇒
      readJournal.eventsByTag("person-created", recoverOffsetPersonCreated)
        .runWith(Sink.actorRefWithAck(self, Init(), Ack(), Completed()))
  }

  override def receiveCommand: Receive = LoggingReceive {
    case _: Completed ⇒ context.stop(self)

    case _: Init      ⇒ sender() ! Ack() // provide demand

    case EventEnvelope(offset, pid, seqno, PBPersonCreated(firstName, lastName, timestamp)) ⇒
      // side effect only in command handler
      val theSender = sender()
      personDao.savePerson(pid, firstName, lastName).map { _ ⇒
        persist(PersonInserted(pid))(handleEvent)
        self ! SavePersonSucceeded(offset, theSender)
      }

    case SavePersonSucceeded(offset, theSender) ⇒
      persist(PersonHandled(offset))(handleEvent)
      theSender ! Ack() // get next message
  }
}

object UpdatePersonFirstNameHandler {
  sealed trait Event
  final case class PersonHandled(offset: Long) extends Event
  final case class PersonAggregated(pid: String, firstname: String) extends Event
}

/**
 * Aggregates PersonCreated and FirstnameChanged for a certain persistence id
 * It will only persist a PersonAggregated event
 */
class UpdatePersonFirstNameAggregator extends PersistentActor {
  override def persistenceId: String = "UpdatePersonFirstNameHandler"

  var recoverOffsetPersonCreated: Long = 0
  var recoverOffsetFirstNameChanged: Long = 0

  var query: NotUsed = null

  override def receiveRecover: Receive = {
    case RecoveryCompleted ⇒
  }

  override def receiveCommand: Receive = {
    case _ ⇒
  }
}

object LaunchPerson extends App {
  val configName = "person-application.conf"
  lazy val configuration = ConfigFactory.load(configName)
  implicit val system: ActorSystem = ActorSystem("ClusterSystem", configuration)
  sys.addShutdownHook(system.terminate())
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  lazy val readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  // launch the personShardRegion; the returned actor must be used to send messages to the shard
  val personRegion: ActorRef = ClusterSharding(system).start(
    typeName = Person.PersonShardName,
    entityProps = Props[Person],
    settings = ClusterShardingSettings(system),
    extractEntityId = Person.extractEntityId,
    extractShardId = Person.extractShardId
  )

  val supportDesk = system.actorOf(Props(new SupportDesk(personRegion, readJournal)))

  val personReadModelDatabase = slick.jdbc.JdbcBackend.Database.forConfig("person-read-model", system.settings.config)

  val personDao = new PersonDaoImpl(personReadModelDatabase, slick.driver.PostgresDriver)

  val insertPersonInPersonTableHandler = system.actorOf(Props(new InsertPersonInPersonTableHandler(readJournal, personDao)))

  val banner = s"""
    |
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
