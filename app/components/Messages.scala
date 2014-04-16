/*
 * The Ipic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package components

import java.io.File
import akka.actor.ActorRef

/**
 * Define messages for actors
 */
object Messages {
  trait IpicMessage
  case class IpicPayLoad(file : File, contentType: String, key: String, piclet:String, adminToken:String)
  case class GenerateThumbnail(payload: IpicPayLoad) extends IpicMessage
  case class UploadFile(payload: IpicPayLoad) extends IpicMessage
  case class UploadThumb(payload: IpicPayLoad, sender: ActorRef) extends IpicMessage
  case class DeleteFile(key: String, piclet: String) extends IpicMessage
  case object Done extends IpicMessage
  case class Uploaded(sender: ActorRef) extends IpicMessage
}
