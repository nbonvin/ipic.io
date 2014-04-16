/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

import com.google.common.hash.Hashing


object CryptoHelper {
  def md5(text: String) = Hashing.md5().hashString(text).asLong().toString
}
