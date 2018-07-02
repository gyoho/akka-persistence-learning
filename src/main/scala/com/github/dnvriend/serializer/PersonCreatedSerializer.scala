package com.github.dnvriend.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.dnvriend.data.Event.PBPersonCreated

/**
 * Converts PersonCreated Google Protobuf Message
 * to byte array and back
 */
class PersonCreatedSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 105

  final val Manifest = classOf[PBPersonCreated].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) PBPersonCreated.parseFrom(bytes)
    else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: PBPersonCreated ⇒ e.toByteArray
    case _                  ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
