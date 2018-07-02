package com.github.dnvriend.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.dnvriend.Person.CreatePerson
import com.github.dnvriend.data.Command.PBCreatePerson

/**
 * Converts FirstName Google Protobuf Message
 * to byte array and back
 */
class CreatePersonSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 102

  final val Manifest = classOf[CreatePerson].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  /**
   * Unmarshal to the data model
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) {
      val PBCreatePerson(firstName, lastName, timestamp) = PBCreatePerson.parseFrom(bytes)
      CreatePerson(firstName, lastName, timestamp)
    } else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  /**
   * Marshal the data model to bytes
   */
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case CreatePerson(firstName, lastName, timestamp) ⇒ PBCreatePerson(firstName, lastName, timestamp).toByteArray
    case _                                            ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
