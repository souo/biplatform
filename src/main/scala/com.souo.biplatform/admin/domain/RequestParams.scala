package com.souo.biplatform.admin.domain

import java.util.UUID

import com.souo.biplatform.model.{CubeSchema, Dimension, Measure}

/**
 * Created by souo on 2016/12/20
 */
object RequestParams {
  case class CreateCubeParams(name: String, schema: Schema)

  case class UpdateCube(name: String, schema: Schema)

  case class Schema(tableName: String, dimensions: List[Dimension], measures: List[Dimension])

  implicit def schemaToCubeSchema(schema: Schema): CubeSchema = {
    CubeSchema(schema.tableName, schema.dimensions, schema.measures.map { dim ⇒
      Measure(dimension = dim)
    })
  }
}
