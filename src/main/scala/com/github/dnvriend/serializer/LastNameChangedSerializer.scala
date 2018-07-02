package com.github.dnvriend.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.dnvriend.data.Event.{ PBLastNameChanged, PBFirstNameChanged }

/**
 * Converts LastNameChanged Google Protobuf Message
 * to byte array and back.
 */
class LastNameChangedSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 104

  final val Manifest = classOf[PBLastNameChanged].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) PBLastNameChanged.parseFrom(bytes)
    else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: PBLastNameChanged ⇒ e.toByteArray
    case _                    ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
