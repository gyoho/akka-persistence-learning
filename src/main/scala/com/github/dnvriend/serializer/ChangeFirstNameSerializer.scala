package com.github.dnvriend.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.dnvriend.Person.ChangeFirstName
import com.github.dnvriend.data.Command.PBChangeFirstName

/**
 * Converts FirstName Google Protobuf Message
 * to byte array and back
 */
class ChangeFirstNameSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 100

  final val Manifest = classOf[ChangeFirstName].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  /**
   * Unmarshal to the data model
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) {
      val PBChangeFirstName(firstName, timestamp) = PBChangeFirstName.parseFrom(bytes)
      ChangeFirstName(firstName, timestamp)
    } else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  /**
   * Marshal the data model to bytes
   */
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case ChangeFirstName(firstName, timestamp) ⇒ PBChangeFirstName(firstName, timestamp).toByteArray
    case _                                     ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
