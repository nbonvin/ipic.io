/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

import org.joda.time.{Minutes, Hours, Days, DateTime}


object DateHelper {

  def nextMonthDate = new DateTime().plusMonths(1).toDate

  /**
   * pretty print of a date
   * @param modificationDate
   * @return
   */
  def lastModificationPretty(modificationDate:Long) = {
    val start = new DateTime(modificationDate)
    val end = new DateTime()
    val days = Days.daysBetween(start,end).getDays
    val sb = new StringBuffer
    if (days >= 1) {
      sb.append(s"$days day")
      if (days > 1) sb.append("s")
    }
    else {
      val hours = Hours.hoursBetween(start,end).getHours
      if (hours >= 1) {
        sb.append(s" $hours hour")
        if (hours > 1) sb.append("s")
      } else {
        val minutes = Minutes.minutesBetween(start,end).getMinutes
        sb.append(s" $minutes minute")
        if (minutes > 1)sb.append("s")
      }
    }
    sb.append(" ago")
    sb.toString
  }
}
