package com.souo.biplatform.cube

import java.util.UUID

import com.souo.biplatform.model.{CubeSchema, Dimension, Measure}

/**
  * Created by souo on 2016/12/20
  */
object RequestParams {

  case class Add(name: String, schema: Schema)

  case class Update(name: String, schema: Schema)

  case class Schema(tableId: String,
                    dimensions: List[Dimension],
                    measures: List[Dimension],
                    dataSourceId: UUID)

  implicit def schemaToCubeSchema(schema: Schema): CubeSchema = {
    CubeSchema(schema.tableId, schema.dimensions, schema.measures.map { dim ⇒
      Measure(dimension = dim)
    }, schema.dataSourceId)
  }
}
