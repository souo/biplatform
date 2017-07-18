package com.souo.biplatform.cache.serializer

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 *
 * Object input stream which tries the thread local class loader.
 * @author souo
 */
private[serializer] class GenericCodecObjectInputStream(classTag: ClassTag[_], in: InputStream)
    extends ObjectInputStream(in) {

  private def classTagClassLoader = classTag.runtimeClass.getClassLoader

  private def threadLocalClassLoader = {
    Thread.currentThread().getContextClassLoader
  }

  override def resolveClass(desc: ObjectStreamClass): Class[_] = {
    try classTagClassLoader.loadClass(desc.getName) catch {
      case NonFatal(_) ⇒
        try super.resolveClass(desc) catch {
          case NonFatal(_) ⇒
            threadLocalClassLoader.loadClass(desc.getName)
        }
    }
  }
}
