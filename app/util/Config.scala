/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

import play.api.Play

object Config {

  // Max file length. Default : 10MB
  val maxFileLengthInMB = Play.current.configuration.getInt("ipic.maxFileLengthInMB").getOrElse(10)

  // Default size in pixel of a thumbnail
  val thumbSize = 150
}
