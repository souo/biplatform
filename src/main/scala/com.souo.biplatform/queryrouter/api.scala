package com.souo.biplatform.queryrouter

import cats.free.Free

/**
 * @author souo
 */
object api {
  implicit def implicitLift[F[_], A](fa: F[A]): Free[F, A] = Free liftF fa

  def execute[F[_], R](m: F[R]): Free[F, R] = m.step
}
