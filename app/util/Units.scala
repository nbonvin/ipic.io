/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

object Units {
  protected val unit = 1024

  val Byte = 1L
  val KB = Byte * unit
  val MB = KB * unit
  val GB = MB * unit
  val TB = GB * unit

  /**
   * pretty print a size
   * @param size
   * @return
   */
  def prettyPrint(size:Long) = {
    val idx = (math.log(size) / math.log(Units.unit)).toInt
    Seq("bytes", "KB", "MB", "GB", "TB", "PB").lift(idx).map { unit =>
      ("%.2f " format (size / math.pow(Units.unit, idx))) + unit
    }.getOrElse(size + " bytes")
  }
}