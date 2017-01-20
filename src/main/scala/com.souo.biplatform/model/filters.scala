package com.souo.biplatform.model

/**
 * Created by souo on 2016/12/21
 */
sealed trait Filter

case class gt(dimension: Dimension, value: String) extends Filter

case class gte(dimension: Dimension, value: String) extends Filter

case class lt(dimension: Dimension, value: String) extends Filter

case class lte(dimension: Dimension, value: String) extends Filter

case class eql(dimension: Dimension, value: String) extends Filter

case class in(dimension: Dimension, value: Seq[String]) extends Filter

case class range(dimension: Dimension, value: Seq[String]) extends Filter

object Filter

