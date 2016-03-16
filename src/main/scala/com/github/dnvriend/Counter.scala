/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import akka.actor.{ ActorSystem, Props }
import akka.persistence.PersistentActor
import akka.persistence.jdbc.query.journal.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
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

object Counter extends App {
  val configName = "counter-application.conf"
  lazy val configuration = ConfigFactory.load(configName)
  implicit val system: ActorSystem = ActorSystem("demo", configuration)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  lazy val readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
  val counter = system.actorOf(Props(new CounterActor))

  // async event listener
  readJournal.eventsByPersistenceId(CounterActor.PersistenceId, 0, Long.MaxValue).runForeach {
    case e ⇒ println("Received event: " + e)
  }

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
