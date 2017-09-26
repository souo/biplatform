package com.souo.biplatform.model

import io.swagger.annotations.ApiModelProperty

/**
 * @author souo
 */
object DataType extends Enumeration {
  @ApiModelProperty(dataType        = "string", allowableValues = "STRING,DATE,NUMERIC")
  type DataType = Value
  val STRING, DATE, NUMERIC = Value
}

object DataTypes {

  import DataType._

  def apply(jdbcType: String): DataType = jdbcType.toUpperCase match {
    case "TINYINT" | "SMALLINT" | "INTEGER" | "BIGINT" | "REAL" | "FLOAT" |
      "DOUBLE" | "DECIMAL" | "NUMERIC" | "BIT" ⇒ NUMERIC
    case "DATE" | "TIME" | "TIMESTAMP" ⇒ DATE
    case _                             ⇒ STRING
  }
}