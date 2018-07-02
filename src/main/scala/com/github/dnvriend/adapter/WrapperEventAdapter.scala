package com.github.dnvriend.adapter

import akka.persistence.journal.{ EventSeq, ReadEventAdapter }
import pprint._, Config.Colors.PPrintConfig

class WrapperEventAdapter extends ReadEventAdapter {
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case Wrapper(payload, created) ⇒
      log2("Payload: " + payload)
      log2("Created: " + created)
      EventSeq.single(payload)
  }
}
