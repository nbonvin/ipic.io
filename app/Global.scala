/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

import components.{AmazonS3Component, ThumbnailComponent}

import play.api.{Logger, GlobalSettings}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import ExecutionContext.Implicits.global

object Global extends GlobalSettings {

  /**
   * Start callback
   * @param app
   */
  override def onStart(app:  play.api.Application) {
    Logger.info("iPic.io platform is starting ...")
    ThumbnailComponent.start
    AmazonS3Component.start
  }

  /**
   * Stop callback
   * @param app
   */
  override def onStop(app: play.api.Application) {
    Logger.info("iPic.io platform is stopping ...")
    ThumbnailComponent.stop
    AmazonS3Component.stop
  }

  /**
   * Handler of NofFound pages
   * @param request
   * @return
   */
  override def onHandlerNotFound(request : play.api.mvc.RequestHeader) : Future[SimpleResult] = {
    Future.successful(Results.NotFound(views.html.notFound()))
  }


}

