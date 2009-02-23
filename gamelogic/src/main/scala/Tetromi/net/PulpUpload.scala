/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

package Tetromi.net

import java.net.URL
import java.util.{HashMap, Map}

/**
 * Simple abstract net wrapper for PulpCore's URLConnection-based Upload object.
 * @author will
 * @date Oct 31, 2008 7:51:12 AM
 */

class PulpUpload(url:URL) extends AbstractUpload(url: URL) {
  val u = new Upload(url)
  def addField(field:String, value:String): Unit = u.addField(field,value)
  def sendNow: Unit = u.sendNow
  def getResponse: String = u.getResponse
  def getCookie: Option[String] = {
    val fields = u.getResponseFields.asInstanceOf[Map[String,java.util.List[String]]]
    val c = fields.get("Set-Cookie")
    if (c != null && c.size > 0) {
      Some(c.get(0))
    } else None
  }
}
