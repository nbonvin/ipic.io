/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package controllers

import play.api._
import play.api.mvc._
import util._
import play.api.libs.Crypto
import scala.concurrent.{ ExecutionContext, Future}
import models.iFile
import com.codahale.jerkson.Json
import play.api.libs.iteratee.Enumerator
import com.amazonaws.services.s3.model.AmazonS3Exception
import ExecutionContext.Implicits.global
import java.net.URLDecoder
import play.api.mvc.SimpleResult
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import components.{AmazonS3Component, ThumbnailComponent}
import components.Messages.{IpicPayLoad, DeleteFile, UploadFile, GenerateThumbnail}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object Application extends Controller {

  // Thumbnail dispatcher
  lazy val thumbnailComponent = ThumbnailComponent.system.actorSelection("/user/ThumbnailDispatcher")

  // AmazonS3 dispatcher
  lazy val amazonS3Component = AmazonS3Component.system.actorSelection("/user/AmazonS3Dispatcher")

  // Amazon S3 helper
  val s3 = new AmazonS3Helper

  // Max file length authorized in bytes
  val maxFileLength = Config.maxFileLengthInMB * 1024 * 1024

  /**
   * check acccess for the admin section
   * @param piclet
   * @param adminToken
   * @return
   */
  def AdminAction(piclet:String, adminToken: String) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
      if (Crypto.sign(piclet) == adminToken) block(request) else Future.successful(Forbidden(views.html.forbidden()))
    }
  }

  /**
   * home page
   * @return
   */
  def index = Action {
    val id = Base62.getId()
    val adminToken = Crypto.sign(id)
    Ok(views.html.index(id,adminToken))
  }

  /**
   * List action: choose between public and private piclet
   * @param piclet
   * @param view
   * @param adminToken
   * @return
   */
  def list(piclet : String, view : Option[String] = None, adminToken: Option[String]) = if (adminToken.isDefined) listPrivate(piclet,adminToken.get) else listPublic(piclet,view)

  /**
   * Display public piclet
   * @param piclet
   * @param view
   * @return
   */
  def listPublic(piclet : String,view : Option[String]) = Action {
    val v = view.filter(_.equals("list")).getOrElse("gallery")
    val l = s3.listFiles(piclet)
    if (l.isEmpty) NotFound(views.html.notFound())
    else Ok(views.html.listPiclet(piclet,l,v))
  }

  /**
   * Display private piclet
   * @param piclet
   * @param adminToken
   * @return
   */
  def listPrivate(piclet : String, adminToken: String) = AdminAction(piclet,adminToken) {
    Ok(views.html.listAdminPiclet(piclet,adminToken,s3.listFiles(piclet)))
  }

  /**
   * Upload action
   * @param piclet
   * @param adminToken
   * @return
   */
  def uploadPrivate(piclet : String, adminToken: String) = AdminAction(piclet,adminToken).async (parse.maxLength(maxFileLength, parse.multipartFormData)) { implicit request =>
    request.body match {
      case Left(MaxSizeExceeded(length)) => Future {BadRequest("Your file is too large, we accept just " + length + " bytes!") }
      case Right(multipartForm) => {
        multipartForm.file("file").map { (picture: FilePart[TemporaryFile]) =>
          val filename = picture.filename
          val contentType = picture.contentType.getOrElse("")
          val tmpFile = FileHelper.getTmpFile
          picture.ref.moveTo(tmpFile)
          if (contentType.startsWith("image")) {
            val thumbName = s"${Config.thumbSize}x${Config.thumbSize}-$filename"
            implicit val timeout = Timeout(5 minutes)
            thumbnailComponent ? GenerateThumbnail(IpicPayLoad(tmpFile,contentType,s"$piclet/$thumbName",piclet,adminToken)) map { r=>
              amazonS3Component ! UploadFile(IpicPayLoad(tmpFile, contentType,s"$piclet/$filename",piclet, adminToken))
              val ifile = iFile(name = picture.filename, size = 1000, contentType = contentType, url = Option(routes.Application.getS3Object(piclet,filename).absoluteURL()), thumbnailUrl = Option(routes.Application.getS3Object(piclet,thumbName).absoluteURL()) )
              Ok(Json.generate(ifile))
            }
          } else {
            amazonS3Component ! UploadFile(IpicPayLoad(tmpFile, contentType,s"$piclet/$filename",piclet, adminToken))
            val ifile = iFile(name = picture.filename, size = 1000, contentType = contentType, url = Option(routes.Application.getS3Object(piclet,filename).absoluteURL()), thumbnailUrl = None )
            Future.successful(Ok(Json.generate(ifile)))
          }
        }.getOrElse {
          Future {
            Redirect(routes.Application.index).flashing("error" -> "Missing file")
          }
        }
      }
    }
  }

  /**
   * Delete files
   * @param piclet
   * @param adminToken
   * @return
   */
  def deleteFile(piclet:String, adminToken:String) = AdminAction(piclet,adminToken)(parse.json) { request =>
    (request.body \ "files").as[List[String]].foreach{file =>
      amazonS3Component ! DeleteFile(s"$piclet/$file", piclet)
      val thumbName = s"${Config.thumbSize}x${Config.thumbSize}-$file"
      amazonS3Component ! DeleteFile(s"$piclet/$thumbName", piclet)
    }
    Ok
  }

  /**
   * Read file from Amazon S3
   * @param piclet
   * @param file
   * @return
   */
  def getS3Object(piclet:String, file: String) = Action {
    val key = s"$piclet/" + URLDecoder.decode(file,"UTF-8")
    try {
      val obj = s3.client.getObject(s3.bucket,key)
      val meta = obj.getObjectMetadata
      val adminToken = Crypto.sign(piclet)
      val decryptInput = CipherStream.getDecryptInputStream(adminToken,obj.getObjectContent)
      val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(decryptInput)
      Ok.chunked(dataContent).withHeaders("Content-Type" -> meta.getContentType, "Expires" -> DateHelper.nextMonthDate.toString)
    } catch {
      case e : AmazonS3Exception => {
        Logger.warn(s"Error while getting $key: " + e.getMessage)
        NotFound
      }
    }
  }


  /**
   * Download all files of a piclet in a ZIP file
   * @param piclet
   * @return
   */
  def getS3ZippedObjects(piclet:String) = Action {
    import play.api.libs.iteratee._
    import java.util.zip._

    val enumerator = Enumerator.outputStream { os =>
      val zip = new ZipOutputStream(os)
      s3.listFiles(piclet).map{ iFile =>
        zip.putNextEntry(new ZipEntry(s"$piclet/${iFile.name}"))
        val obj = s3.client.getObject(s3.bucket,s"$piclet/${iFile.name}")
        val adminToken = Crypto.sign(piclet)
        val decryptInput = CipherStream.getDecryptInputStream(adminToken,obj.getObjectContent)
        //Iterator continually decryptInput.read takeWhile (-1 !=) map (zip.write)
        var b = decryptInput.read()
        while(b != -1){
          zip.write(b)
          b = decryptInput.read()
        }
        zip.closeEntry()
      }
      zip.close()
    }
    Ok.chunked(enumerator >>> Enumerator.eof).withHeaders(
      "Content-Type"->"application/zip",
      "Content-Disposition"->s"attachment; filename=ipic.io-$piclet.zip"
    )
  }

}