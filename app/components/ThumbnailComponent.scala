/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package components

import akka.actor.{ActorSystem, Actor, Props}
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import play.api.Logger
import com.typesafe.config.ConfigFactory
import components.Messages._
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import util.{Config, FileHelper}
import components.Messages.UploadThumb
import components.Messages.GenerateThumbnail
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext


/**
 * Actor responsible for creating thumbnails
 */
class ThumbnailActor (implicit dispatcher: ExecutionContext) extends Actor {

  lazy val amazonS3Component = AmazonS3Component.system.actorSelection("/user/AmazonS3Dispatcher")
  implicit val timeout = Timeout(30 seconds)

  def receive = {
    case GenerateThumbnail(IpicPayLoad(file, contentType, key, piclet, adminToken)) =>  {
      val src = ImageIO.read(file)
      if (src == null) {
        Logger.warn(s"unable to read file for key $key: " + file.getAbsoluteFile)
        sender ! Done
      } else {
        val thumb = Scalr.resize(src, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_HEIGHT, Config.thumbSize)
        val tmpFile = FileHelper.getTmpFile
        ImageIO.write(thumb, contentType.replaceAll("image/",""), tmpFile)
        amazonS3Component ? UploadThumb(IpicPayLoad(tmpFile, contentType,key,piclet,adminToken),sender) onSuccess { case Uploaded(sender) =>
            sender ! Done
        }
      }
    }
  }

}

/**
 * Dispatcher
 * @param nrOfWorkers
 */
class ThumbnailDispatcher(nrOfWorkers: Int = 2)(implicit dispatcher: ExecutionContext) extends Actor {
  // Msg router
  val msgHandler = context.actorOf(Props(new ThumbnailActor).withRouter(SmallestMailboxRouter(nrOfWorkers)), name = "ThumbnailActor")

  def receive = {
    case msg : IpicMessage =>  msgHandler forward msg
    case _  =>  Logger.error("Wrong IpicMessage received at dispatcher")
  }
}

/**
 * Component
 */
object ThumbnailComponent {

  val system = ActorSystem("ThumbnailSystem", ConfigFactory.defaultReference())
  implicit val dispatcher = system.dispatcher

  def start = {
    Logger.info("Thumbnail component starting ... ")
    system.actorOf(Props(new ThumbnailDispatcher), name = "ThumbnailDispatcher")
  }

  def stop = {
    Logger.info("Thumbnail component stopping ... ")
    system.shutdown()
  }
}