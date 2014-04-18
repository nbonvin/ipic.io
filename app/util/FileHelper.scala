/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

import java.io.File

object FileHelper {

  val idLock = new AnyRef

  /**
   * List recursively all files in a folder
   * @param f
   * @return
   */
  def recursiveListFiles(f: File): List[File] = {
    if (f.exists()){
      val these = f.listFiles.filterNot(_.getName.endsWith(".small.png")).toList
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    } else {
      Nil
    }
  }

  /**
   * Generate a unique file
   * @return
   */
  def getTmpFile = new File(Config.tempFolder + idLock.synchronized { System.nanoTime() } + ".tmp")
}
