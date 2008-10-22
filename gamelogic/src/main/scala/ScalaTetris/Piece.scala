package ScalaTetris

import java.util.Calendar
import scala.collection.immutable.{Set, Map}
import scala.collection.mutable
import scala.Random

// Fixed clockwise order
object PieceRotation extends Enumeration("0", "90", "180", "270") {
  val Zero, Rot90, Rot180, Rot270 = Rot
  class Rot(i: Int, name: String) extends Val(i, name) {
    def +(rot:Rotation.Value):Rot = PieceRotation.apply((id + rot.id) % maxId).asInstanceOf[Rot]
  }
  def Rot: Rot = new Rot(nextId, if (nextName.hasNext) nextName.next else null)
  def apply(rot: Value): Double = rot match {
    case Zero => 0
    case Rot90 => Math.Pi * 0.5
    case Rot180 => Math.Pi
    case Rot270 => Math.Pi * 1.5
    case _ => 0
  }
 }

object PieceType extends Enumeration("#I#","#S#","#J#","#Z#","#L#","#O#","#T#","Empty") {
  val I,S,J,Z,L,O,T,Empty = Value
}

// Piece orientations are listed in clockwise order starting from zero
case class PieceDef(typeDef : PieceType.Value,
                   piece0 : Array[(Int,Int)],
                   piece90 : Array[(Int,Int)],
                   piece180 : Array[(Int,Int)],
                   piece270 : Array[(Int,Int)])

object TetrisRotation { val Table = Set(
// Note: Pieces are defined in fixed-order Arrays so that the Blocks remain consistent through rotation!
  PieceDef(PieceType.I,
          Array((0, 1), (1, 1), (2, 1), (3, 1)),
          Array((2, 0), (2, 1), (2, 2), (2, 3)),
          Array((3, 1), (2, 1), (1, 1), (0, 1)),
          Array((2, 3), (2, 2), (2, 1), (2, 0))
          ),
  PieceDef(PieceType.L,
          Array((0, 2), (0, 1), (1, 1), (2, 1)),
          Array((0, 0), (1, 0), (1, 1), (1, 2)),
          Array((2, 1), (2, 2), (1, 2), (0, 2)),
          Array((2, 2), (1, 2), (1, 1), (1, 0))
          ),
  PieceDef(PieceType.J,
          Array((0, 1), (1, 1), (2, 1), (2, 2)),
          Array((1, 0), (1, 1), (1, 2), (0, 2)),
          Array((2, 2), (1, 2), (0, 2), (0, 1)),
          Array((1, 2), (1, 1), (1, 0), (2, 0))
          ),
  PieceDef(PieceType.T,
          Array((0, 1), (1, 1), (2, 1), (1, 2)),
          Array((1, 0), (1, 1), (1, 2), (0, 1)),
          Array((2, 2), (1, 2), (0, 2), (1, 1)),
          Array((1, 2), (1, 1), (1, 0), (2, 1))
          ),
  PieceDef(PieceType.S,
          Array((0, 2), (1, 2), (1, 1), (2, 1)),
          Array((0, 0), (0, 1), (1, 1), (1, 2)),
          Array((2, 1), (1, 1), (1, 2), (0, 2)),
          Array((1, 2), (1, 1), (0, 1), (0, 0))
          ),
  PieceDef(PieceType.Z,
          Array((0, 1), (1, 1), (1, 2), (2, 2)),
          Array((2, 0), (2, 1), (1, 1), (1, 2)),
          Array((2, 2), (1, 2), (1, 1), (0, 1)),
          Array((1, 2), (1, 1), (2, 1), (2, 0))
          ),
  PieceDef(PieceType.O,
          Array((1, 1), (2, 1), (2, 2), (1, 2)),
          Array((2, 1), (2, 2), (1, 2), (1, 1)),
          Array((2, 2), (1, 2), (1, 1), (2, 1)),
          Array((1, 2), (1, 1), (2, 1), (2, 2))
          )
  )

  // Relative position offsets of the four blocks mapped to their four possible rotations.
   val blockMap = mutable.Map.empty[PieceType.Value,Map[PieceRotation.Value,Array[(Int,Int)]]]
   for(pieceDef <- TetrisRotation.Table) {
     blockMap(pieceDef.typeDef) = Map(
             PieceRotation.Zero -> pieceDef.piece0,
             PieceRotation.Rot90 -> pieceDef.piece90,
             PieceRotation.Rot180 -> pieceDef.piece180,
             PieceRotation.Rot270 -> pieceDef.piece270
             )}

  /** Return an Array of piece position offsets for a given piece type + rotation */
  def piece(pieceType : PieceType.Value, rotation : PieceRotation.Value): Array[(Int,Int)] = {
    if(pieceType == PieceType.Empty) new Array[(Int,Int)](0)
    else blockMap(pieceType)(rotation)
  }
}

/** A distinct tetromino piece containing four Blocks with relative position offsets. */
class Piece(pType: PieceType.Value, rot : PieceRotation.Rot) {
  // Default initialization to the "zero" orientation
  def this(pieceType : PieceType.Value) = this(pieceType, PieceRotation.Zero)

  type Pos = (Int,Int)

  val pieceType = pType
  var rotation = rot

  // Create the Array of Blocks + relative offsets contained by this Piece.
  var positions : Array[Pos] = TetrisRotation.piece(pieceType,rotation)
  var blocks : Array[Block] = positions.map(x => new Block(BlockType(pieceType)))
  blocks.map(b => {b.isActive = true; b.rotation = rotation})

  def blockSet : Array[(Block, Pos)] = blocks.zip(positions)
  
  /** Returns a new blockSet for this piece according to the specified rotation. */
  def checkRotate(rot: Rotation.Value): Array[(Block,Pos)] = blocks.zip(_checkRotate(rot))
  private def _checkRotate(rot: Rotation.Value) = TetrisRotation.piece(pieceType,rotation + rot)

  /** Performs a rotation on this Piece. */
  def doRotate(rot: Rotation.Value) = {
    positions = _checkRotate(rot)
    rotation = rotation + rot
    blocks.map(_.rotation = rotation)
    blockSet
  }

  /** Locks all of the blocks in this Piece to the playfield */
  def lock = blocks.map(_.isActive = false)

  override def toString = {
    val str2 = for ((block, pos) <- blockSet) yield { "block=" + block + ",pos=" + pos }
    "Piece "+pieceType+":" + str2.toString
  }
}

object EmptyPiece extends Piece(PieceType.Empty)

class PieceQueue(r : Randomizer.Value, val seed : Long) {
  val queueLength = 7
  var rand: Randomizer = Randomizer(r,seed)
  val pieceQueue = new mutable.Queue[Piece];
  fillQueue
  
  /** Resets the piece queue with a new Randomizer. */
  def reset(r : Randomizer.Value, seed : Long) = {
    rand = Randomizer(r,seed)
    pieceQueue.clear
    fillQueue
  }

  private def fillQueue = { for(i <- 0 until queueLength) pieceQueue += nextPiece }

  def nextPiece = new Piece(rand.getNextPiece)
  /** Grabs the next piece from the queue. */
  def apply(): Piece = { pieceQueue += nextPiece; pieceQueue.dequeue }
  /** Peeks ahead at the next Xth piece in the queue. */
  def apply(i: Int): Piece = pieceQueue(i)
}