package com.souo.biplatform

import cats.{SemigroupK, _}
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, _}
import cats.implicits._

/**
 * Created by souo on 2017/1/6
 */
package object model {

  implicit val nelSemigroup: Semigroup[NonEmptyList[String]] = {
    SemigroupK[NonEmptyList].algebra[String]
  }

  implicit val validatedNelSemigroup: Semigroup[ValidatedNel[String, Boolean]] = {
    new Semigroup[ValidatedNel[String, Boolean]] {
      override def combine(
        a: ValidatedNel[String, Boolean],
        b: ValidatedNel[String, Boolean]
      ): ValidatedNel[String, Boolean] = {
        (a, b) match {
          case (Valid(x), Valid(y))   ⇒ Valid(x && y)
          case (Invalid(x), Valid(y)) ⇒ Invalid(x)
          case (Valid(x), Invalid(y)) ⇒ Invalid(y)
          case (Invalid(x), Invalid(y)) ⇒
            Invalid(x |+| y)
        }
      }
    }
  }
}
