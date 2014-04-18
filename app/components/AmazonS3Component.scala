/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package components

import akka.actor.{ActorSystem, Actor, Props}
import akka.routing.SmallestMailboxRouter
import play.api.Logger
import com.typesafe.config.ConfigFactory
import components.Messages._
import util.AmazonS3Helper
import play.api.cache.Cache
import play.api.Play.current
import components.Messages.UploadThumb
import components.Messages.DeleteFile
import components.Messages.UploadFile
import scala.concurrent.ExecutionContext


/**
 * Actor dealing with operations to Amazon S3
 */
class AmazonS3Actor(implicit dispatcher: ExecutionContext) extends Actor {

  val s3 = new AmazonS3Helper

  def receive = {
    case msg @ UploadFile(IpicPayLoad(file, contentType, key, piclet, adminToken))  =>  {
      Logger.debug("Message received at AmazonS3Actor: " + msg)
      s3.uploadFile(file, contentType, key, adminToken)
      Cache.remove(piclet)
    }
    case msg @ UploadThumb(IpicPayLoad(file, contentType, key, piclet, adminToken), initiator)  =>  {
      Logger.debug("Message received at AmazonS3Actor: " + msg)
      s3.uploadFile(file, contentType, key, adminToken)
      Cache.remove(piclet)
      sender ! Uploaded(initiator)
    }

    case msg @ DeleteFile(key,piclet)  =>  {
      Logger.debug("Message received at AmazonS3Actor: " + msg)
      s3.deleteFile(key)
      Cache.remove(piclet)
    }

    case msg @ _ => Logger.warn("Unknown message received at AmazonS3Actor: " + msg)
  }

}

/**
 * Dispatcher Actor
 */
class AmazonS3Dispatcher(implicit dispatcher: ExecutionContext) extends Actor {
  // Msg router
  val thumbHandler = context.actorOf(Props(new AmazonS3Actor).withRouter(SmallestMailboxRouter(10)), name = "thumbHandler")
  val fileHandler = context.actorOf(Props(new AmazonS3Actor).withRouter(SmallestMailboxRouter(5)), name = "fileHandler")

  def receive = {
    case msg : UploadFile =>  fileHandler forward msg
    case msg : DeleteFile =>  fileHandler forward msg
    case msg : UploadThumb =>  thumbHandler forward msg
    case msg @ _  =>  Logger.error("Wrong IpicMessage received at dispatcher: " + msg)
  }
}

/**
 * AmazonS3 component
 */
object AmazonS3Component {

  val system = ActorSystem("AmazonS3System", ConfigFactory.defaultReference())
  implicit val dispatcher: ExecutionContext = system.dispatcher

  def start = {
    Logger.info("AmazonS3 component starting ... ")
    system.actorOf(Props(new AmazonS3Dispatcher), name = "AmazonS3Dispatcher")
  }

  def stop = {
    Logger.info("AmazonS3 component stopping ... ")
    system.shutdown()
  }
}