/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util


import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import play.api.{Logger, Play}
import java.io.{File, FileInputStream}
import com.amazonaws.services.s3.model._
import scala.collection.JavaConversions._
import models.iFile
import play.api.cache.Cache
import play.api.Play.current

class AmazonS3Helper {
  val accessKey = Play.current.configuration.getString("aws.access.key").getOrElse("")
  val secretKey = Play.current.configuration.getString("aws.secret.key").getOrElse("")
  val bucket = Play.current.configuration.getString("aws.s3.bucket").getOrElse("ipic.io")
  val host = "https://s3-eu-west-1.amazonaws.com/" + bucket + "/"
  val awsCredentials = new BasicAWSCredentials(accessKey, secretKey)
  val client = new AmazonS3Client(awsCredentials)


  /**
   * Upload a file to Amazon S3
   * @param file
   * @param mime
   * @param key
   * @param adminToken
   */
  def uploadFile(file : File, mime: String, key: String, adminToken: String) = {
    val start = System.currentTimeMillis()
    Logger.debug("key: " + key)
    val maxRetry = 3
    var retryCount = 0
    var success = false
    val meta = new ObjectMetadata
    meta.setContentType(mime)
    //meta.addUserMetadata("descr", "file description")
    do {
      try {
        val in = new FileInputStream(file)
        val cipheredInput = CipherStream.getEncryptInputStream(adminToken, in)
        val req = new PutObjectRequest(bucket, key ,cipheredInput, meta)
        req.setStorageClass(StorageClass.ReducedRedundancy)
        req.setCannedAcl(CannedAccessControlList.PublicRead)
        client.putObject(req)
        success = true
      } catch {
        case e : Exception => {
          Logger.warn(s"unable to upload $key (retry count: $retryCount): " + e.getMessage )
          retryCount += 1
          if (retryCount == 3) throw e
        }
      }
    } while (!success && retryCount < maxRetry)
    file.delete
    val total = System.currentTimeMillis() - start
    Logger.debug(s"Upload of $key took $total ms")
  }

  /**
   * Delete a file from AmazonS3
   * @param key
   */
  def deleteFile(key:String) = {
    client.deleteObject(bucket,key)
  }

  /**
   * List all files inside a bucket
   * @param piclet
   * @return
   */
  def listFiles(piclet:String): List[iFile] = {

    val folder = piclet + "/"
    Cache.getOrElse[List[iFile]](piclet) {
      val objects: ObjectListing = client.listObjects(bucket, folder)
      objects.getObjectSummaries.filterNot(_.getKey.replaceAll(folder,"").startsWith(s"${Config.thumbSize}x${Config.thumbSize}")).map{o : S3ObjectSummary =>
         val name =  o.getKey.replaceAll(folder,"")
         val contentType = name.toLowerCase match {
           case str if str.endsWith(".png") => "image/png"
           case str if str.endsWith(".jpeg") => "image/jpeg"
           case str if str.endsWith(".jpg") => "image/jpeg"
           case str if str.endsWith(".gif") => "image/gif"
           case _ => ""
         }
         val thumb = if (contentType.startsWith("image")) Option(s"${folder}${Config.thumbSize}x${Config.thumbSize}-$name") else None
         iFile(name,contentType,o.getSize,o.getLastModified.getTime, Option(s"$folder$name"),thumb)
      }.toList
    }
  }

}
