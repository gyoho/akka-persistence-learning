package com.github.dnvriend.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.dnvriend.Person.ChangeLastName
import com.github.dnvriend.data.Command.PBChangeLastName

/**
 * Converts FirstName Google Protobuf Message
 * to byte array and back
 */
class ChangeLastNameSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 101

  final val Manifest = classOf[ChangeLastName].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  /**
   * Unmarshal to the data model
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) {
      val PBChangeLastName(lastName, timestamp) = PBChangeLastName.parseFrom(bytes)
      ChangeLastName(lastName, timestamp)
    } else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  /**
   * Marshal the data model to bytes
   */
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case ChangeLastName(lastName, timestamp) ⇒ PBChangeLastName(lastName, timestamp).toByteArray
    case _                                   ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
