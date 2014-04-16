/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

object Base62 {

  val idLock = new AnyRef
  val baseString: String = ((0 to 9) ++ ('A' to 'Z') ++ ('a' to 'z')).mkString
  val base = baseString.length

  /**
   * Get a unique Id
   * @return
   */
  def getId() = {
    val id = idLock.synchronized { System.nanoTime() }
    encode(id)
  }

  /**
   * Encode to base 62
   * @param i
   * @return
   */
  private def encode(i: Long): String = {
    @scala.annotation.tailrec
    def div(i: Long, res: List[Int] = Nil): List[Int] = {
      (i / base) match {
        case q if q > 0 => div(q, (i % base).toInt :: res)
        case _ => i.toInt :: res
      }
    }
    div(i).map(x => baseString(x)).mkString
  }

}

