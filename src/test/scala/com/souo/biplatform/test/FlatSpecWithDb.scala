package com.souo.biplatform.test

import com.souo.biplatform.common.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
 * Created by souo on 2016/12/19
 */
trait FlatSpecWithDb extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with IntegrationPatience {

  private val connectionString = "jdbc:h2:mem:biplatform" + this.getClass.getSimpleName + ";DB_CLOSE_DELAY=-1"

  val databaseConfig = new DatabaseConfig {
    override def rootConfig: Config = ConfigFactory.parseString(
      s"""
         |designer {
         |  db {
         |    h2 {
         |    queueSize = 3000
         |     numThreads = 4
         |     dataSourceClass = "org.h2.jdbcx.JdbcDataSource"
         |      properties = {
         |        url = "$connectionString"
         |      }
         |    }
         |  }
         |}
       """.stripMargin
    )
  }

  val sqlDatabase = SqlDatabase.create(databaseConfig)

  override protected def beforeAll() {
    super.beforeAll()
    createAll()
  }

  def clearData() {
    dropAll()
    createAll()
  }

  override protected def afterAll() {
    super.afterAll()
    dropAll()
    sqlDatabase.close()
  }

  private def createAll() {
    sqlDatabase.updateSchema()
  }

  private def dropAll() {
    import sqlDatabase.driver.api._
    sqlDatabase.db.run(sqlu"DROP ALL OBJECTS").futureValue
  }

}
