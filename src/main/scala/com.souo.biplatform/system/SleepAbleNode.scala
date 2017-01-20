package com.souo.biplatform.system

import akka.actor.Cancellable

import scala.concurrent.duration._
/**
 * Created by souo on 2016/12/26
 */
trait SleepAbleNode extends Node {

  import context.dispatcher

  var sleepTask: Option[Cancellable] = None

  val sleepAfter: Int = 0

  /**
   * Called every time a command is run, only if sleeping is enabled
   */
  def sleep(): Unit = {
    if (sleepAfter > 0) {
      val when = sleepAfter.milliseconds
      sleepTask.foreach(_.cancel())
      sleepTask = Some(context.system.scheduler.scheduleOnce(when) {
        self ! Sleep
      })
    }
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    sleep()
  }

}
