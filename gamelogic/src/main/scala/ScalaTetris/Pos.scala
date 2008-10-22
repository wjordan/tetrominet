package ScalaTetris

/**
 * @author will
 * @date Oct 4, 2008 7:12:03 AM
 */

/** Simple generic 2D 'position' tuple with a few helper functions. */
case class Pos(xx : Double, yy : Double) {
  def this(xy : (Int,Int)) = this(xy._1,xy._2)
  val x = xx; val y = yy;

  def *(scale : Double) = Pos(x*scale,y*scale)
  def *(scaleX : Double, scaleY : Double) = Pos(x*scaleX,y*scaleY)

  def +(pos : Pos):Pos = Pos(x+pos.x,y+pos.y)
  def -(pos : Pos):Pos = Pos(x-pos.x,y-pos.y)

  override def toString = { "("+x+","+y+")" }
  override def equals(other: Any): Boolean = {
    other match {
      case that: Pos => (x == that.x && y == that.y)
      case that: (Int, Int) => (x == that._1 && y == that._2)
      case _ => false
    }
  }
  
  implicit def fromTuple(xy: (Int,Int)): Pos = Pos(xy._1,xy._2)
  implicit def fromIntInt(x: Int,y: Int): Pos = Pos(x,y)
  implicit def toIntInt(pos: Pos): (Int,Int) = (pos.x.toInt,pos.y.toInt)
}