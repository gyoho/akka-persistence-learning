package com.github.dnvriend.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.dnvriend.data.Event.PBFirstNameChanged

/**
 * Converts FirstName Google Protobuf Message
 * to byte array and back
 */
class FirstNameChangedSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 103

  final val Manifest = classOf[PBFirstNameChanged].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  /**
   * Unmarshal to the data model
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) PBFirstNameChanged.parseFrom(bytes)
    else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  /**
   * Marshal the data model to bytes
   */
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: PBFirstNameChanged ⇒ e.toByteArray
    case _                     ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
