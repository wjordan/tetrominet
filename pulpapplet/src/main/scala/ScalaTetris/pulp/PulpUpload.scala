package ScalaTetris.pulp


import java.net.URL


import java.util.{HashMap, Map}
import net.AbstractUpload
import pulpcore.net.Upload

/**
 * Simple abstract net wrapper for PulpCore's Upload object.
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
