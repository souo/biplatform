package com.souo.biplatform.system

import akka.testkit.ImplicitSender
import com.souo.biplatform.model.ReportMeta
import com.souo.biplatform.system.UserNode.{CreateReport, UpdateReport}
import com.souo.biplatform.test.PersistenceSpec
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

object UserNodeSpec {
  val designerConfig = ConfigFactory.parseString(
    """
      |designer {
      |  report{
      |    sleep-after = 2 seconds
      |  }
      |  user {
      |    sleep-after = 2 seconds
      |  }
      |}
    """.stripMargin
  )

  val levelDbPersistentConfig = {
    designerConfig.withFallback(PersistenceSpec.config("leveldb", s"${getClass.getName}_levelDb"))
  }
  val inmemPersistentConfig = {
    designerConfig.withFallback(PersistenceSpec.config("inmem", s"${getClass.getName}_inmem"))
  }

  val kryoConfig = {
    designerConfig.withFallback(
      PersistenceSpec.config("leveldb", s"${getClass.getName}_levelDb_kryo")
    ).withFallback(
        ConfigFactory.load("kryo")
      )
  }

}

/**
 * Created by souo on 2016/12/26
 */
abstract class UserNodeSpec(config: Config) extends PersistenceSpec(config) with ImplicitSender {
  /**
   * Prefix for generating a unique name per test.
   */
  override def namePrefix: String = "user"

  "the user" should {
    "can create reports and list all" in {
      val userNode = system.actorOf(UserNode.props, UserNode.name(name))

      val reportCreate1 = CreateReport(name, "report1")
      val reportCreate2 = CreateReport(name, "report2")
      val reportCreate3 = CreateReport(name, "report3")
      var message: Array[ReportMeta] = Array.empty[ReportMeta]

      userNode ! reportCreate1
      userNode ! reportCreate2
      userNode ! reportCreate3

      receiveWhile(1000 millis) {
        case Right(msg: Array[ReportMeta]) ⇒
          message = msg
      }
      message.length should be (3)
      message.exists(m ⇒ m.createBy == name && m.name == "report1") should be (true)
      message.exists(m ⇒ m.createBy == name && m.name == "report2") should be (true)
      message.exists(m ⇒ m.createBy == name && m.name == "report3") should be (true)

    }

    "can update report" in {
      val userNode = system.actorOf(UserNode.props, UserNode.name(name))
      val reportCreate1 = CreateReport(name, "report1")
      val reportCreate2 = CreateReport(name, "report2")
      val reportCreate3 = CreateReport(name, "report3")
      userNode ! reportCreate1
      userNode ! reportCreate2
      userNode ! reportCreate3

      var message: Array[ReportMeta] = Array.empty[ReportMeta]
      receiveWhile(1000 millis) {
        case Right(msg: Array[ReportMeta]) ⇒
          message = msg
      }
      message.length should be(3)
      message.foreach{ meta ⇒
        userNode ! UpdateReport(name, meta.id, meta.name + "_update")
      }
      receiveWhile(1000 millis) {
        case Right(msg: Array[ReportMeta]) ⇒
          message = msg
      }

      message.length should be(3)
      message.exists(m ⇒ m.createBy == name && m.name == "report1_update") should be (true)
      message.exists(m ⇒ m.createBy == name && m.name == "report2_update") should be (true)
      message.exists(m ⇒ m.createBy == name && m.name == "report3_update") should be (true)
    }

    "can auto sleep after duration  setting of ' designer.user.sleep-after''" in {
      val userNode = system.actorOf(UserNode.props, UserNode.name(name))
      watch(userNode)
      //the "designer.user.sleep-after=2 seconds"
      expectTerminated(userNode, 3 seconds)
    }

  }
}

class InmemUserNodeSpec extends UserNodeSpec(UserNodeSpec.inmemPersistentConfig)

class LevelDbUserNodeSpec extends UserNodeSpec(UserNodeSpec.levelDbPersistentConfig)
