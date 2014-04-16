/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package models

import util.{DateHelper, Units, CryptoHelper}

/*
  Define an iPic file
 */

case class iFile (id: String, name: String, contentType: String, size: Long, modificationDate: Long, url: Option[String], thumbnailUrl: Option[String], modificationDatePretty: String) {
  def lastModifPretty = DateHelper.lastModificationPretty(modificationDate)
  def sizePretty = Units.prettyPrint(size)
}

object iFile {
  def apply (name: String, contentType: String = "", size: Long = 0, modificationDate: Long = System.currentTimeMillis(), url: Option[String] = None, thumbnailUrl: Option[String] = None) = {
    new iFile(CryptoHelper.md5(name), name, contentType, size, modificationDate, url, thumbnailUrl, DateHelper.lastModificationPretty(modificationDate))
  }
}

