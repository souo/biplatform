package com.souo.biplatform.cache.serializer

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, Closeable, ObjectOutputStream}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * @author souo
 */
trait JavaSerializationCodec {
  /**
   * Uses plain Java serialization to deserialize objects
   */
  implicit def javaBinaryCodec[S <: Serializable](implicit ev: ClassTag[S]): Codec[S, Array[Byte]] = {
    new JavaSerializationAnyCodec[S](ev)
  }

}

class JavaSerializationAnyCodec[S <: Serializable](classTag: ClassTag[S]) extends Codec[S, Array[Byte]] {

  def using[T <: Closeable, R](obj: T)(f: T ⇒ R): R =
    try
      f(obj)
    finally
      try obj.close() catch {
        case NonFatal(_) ⇒ // does nothing
      }

  def serialize(value: S): Array[Byte] =
    using (new ByteArrayOutputStream()) { buf ⇒
      using (new ObjectOutputStream(buf)) { out ⇒
        out.writeObject(value)
        out.close()
        buf.toByteArray
      }
    }

  def deserialize(data: Array[Byte]): S =
    using (new ByteArrayInputStream(data)) { buf ⇒
      val in = new GenericCodecObjectInputStream(classTag, buf)
      using (in) { inp ⇒
        inp.readObject().asInstanceOf[S]
      }
    }
}

object JavaSerializationCodec extends JavaSerializationCodec

