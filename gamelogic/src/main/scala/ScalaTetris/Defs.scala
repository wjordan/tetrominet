package ScalaTetris
/**
 * The set of enumerations and type definitions.
 * @author will
 * @date Oct 7, 2008 5:22:51 AM
 */

/** Enumerations for the types of blocks and pieces */
object BlockType extends Enumeration("<I>","<S>","<J>","<Z>","<L>","<O>","<T>") {
  val I,S,J,Z,L,O,T = Value
  val Garbage = Value("Garbage")
  val Empty = Value("Empty")

  // Allows conversion from PieceType to BlockType (Requires identical enumeration order!)
  def apply(p: PieceType.Value): BlockType.Value = apply(p.id)
}

/** User input enumerations */

// Fixed clockwise order
object Rotation extends Enumeration("None","RotCW","Rot180","RotCCW") { val None, RotCW, Rot180, RotCCW = Value }

object Movement extends Enumeration("Left","Right","None") { val Left, Right, None = Value;
  def apply(m: Movement.Value):Pos = m match {
    case Left => Pos(-1,0); case Right => Pos(1,0); case None => Pos(0,0)
  }
}

object Direction extends Enumeration("North", "South", "East", "West") {
  val North, South, East, West = Dir
  /** Extend the default enumeration to define arithmetic rotation operations */
  class Dir(i: Int, name: String) extends Val(i, name) {
    def +(r: PieceRotation.Value): Dir = r match {
      case PieceRotation.Rot90 => this match {
        case Direction.North => Direction.East
        case Direction.East => Direction.South
        case Direction.South => Direction.West
        case Direction.West => Direction.North
      }
      case PieceRotation.Zero => this
      case PieceRotation.Rot180 => (this + PieceRotation.Rot90) + PieceRotation.Rot90
      case PieceRotation.Rot270 => (this + PieceRotation.Rot180) + PieceRotation.Rot90
    }
    def -(r: PieceRotation.Value): Dir = ((this + r) + r) + r
  }
  def Dir: Dir = new Dir(nextId, if (nextName.hasNext) nextName.next else null)
  /** Translate direction into a unit position shift */
  def apply(d: Value): Pos = d match {
    case North => Pos(0, -1); case South => Pos(0, 1); case East => Pos(1, 0); case West => Pos(-1, 0)
  }
}

object Drop extends Enumeration("Hard","Soft","None") { val Hard, Soft, None = Value }
