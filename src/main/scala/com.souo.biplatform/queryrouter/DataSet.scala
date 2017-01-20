package com.souo.biplatform.queryrouter

import com.souo.biplatform.queryrouter.DataCellType.DataCellType

/**
 * Created by souo on 2017/1/5
 */
case class DataSet(rows: List[DataRow])

case class DataCell(
  value:      String,
  `type`:     DataCellType,
  properties: Map[String, String]
)

case class DataRow(cells: List[DataCell])

object DataCellType extends Enumeration {
  type DataCellType = Value
  val ROW_HEADER_HEADER, ROW_HEADER, COLUMN_HEADER, DATA_CELL = Value
}

