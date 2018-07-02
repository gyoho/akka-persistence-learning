package com.github.dnvriend.adapter

import akka.persistence.journal.{ EventAdapter, EventSeq, Tagged }
import com.github.dnvriend.Person._
import com.github.dnvriend.data.Event._

class TaggingEventAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""

  def tag(event: Any, tag: String) = Tagged(event, Set(tag))

  override def toJournal(event: Any): Any = event match {
    case PersonCreated(firstName, lastName, timestamp) ⇒ tag(PBPersonCreated(firstName, lastName, timestamp), "person-created")
    case FirstNameChanged(firstName, timestamp)        ⇒ tag(PBFirstNameChanged(firstName, timestamp), "first-name-changed")
    case LastNameChanged(lastName, timestamp)          ⇒ tag(PBLastNameChanged(lastName, timestamp), "last-name-changed")
    case _                                             ⇒ event
  }

  /**
   * Protobuf messages must be converted back to the domain model
   */
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case PBPersonCreated(firstName, lastName, timestamp) ⇒ EventSeq.single(PersonCreated(firstName, lastName, timestamp))
    case PBFirstNameChanged(firstName, timestamp)        ⇒ EventSeq.single(FirstNameChanged(firstName, timestamp))
    case PBLastNameChanged(lastName, timestamp)          ⇒ EventSeq.single(LastNameChanged(lastName, timestamp))
    case _                                               ⇒ EventSeq.single(event)
  }
}
