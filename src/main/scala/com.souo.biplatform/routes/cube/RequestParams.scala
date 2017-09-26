package com.souo.biplatform.routes.cube

import java.util.UUID

import com.souo.biplatform.model.{CubeSchema, Dimension, Measure}

/**
 * @author souo
 */
object RequestParams {

  case class CubeNameAndSchema(cubeName: String, schema: Schema)
  case class Schema(
    tableName:    String,
    dimensions:   List[Dimension],
    measures:     List[Dimension],
    dataSourceId: UUID)

  implicit def schemaToCubeSchema(schema: Schema): CubeSchema = {
    CubeSchema(schema.tableName, schema.dimensions, schema.measures.map { dim ⇒
      Measure(dimension = dim)
    }, schema.dataSourceId)
  }
}
