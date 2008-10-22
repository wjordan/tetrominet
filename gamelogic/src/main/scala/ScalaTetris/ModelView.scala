package ScalaTetris
/**
 * Contains the primary model
 * @author will
 * @date Oct 5, 2008 6:51:13 AM
 */

/** Model of the Tetrion */
trait TetrionModel extends Model[TetrionView] {
  /** The core play field model is a 2D array of Cells */
  def model : Array[Array[Cell]]
  /** The currently active tetromino Piece */
  var pieceQueue: PieceQueue
  /** The currently active tetromino Piece */
  var piece: Piece
  /** The current lock counter on the active piece */
  var lockCounter: Int
  var gravity : Double
  var gravityCounter : Double
  val blockMap : scala.collection.mutable.Map[Block,Cell]
}

trait TetrionView extends View {
  def notifyCellCreate(c: Cell)
  def pieceCreate(p: Piece)
  def pieceLock(p: Piece)
  def lineClear(b: Array[Cell])
  def updateState
  def lineClearEnd
}

/** A single unit position within the Tetrion that may hold a single block. */
trait Cell extends Model[CellView] {
  /** Fixed position index of this cell within the Tetrion */
  val pos : Pos
  /** The Current Block contained within this Cell, if any */
  var block : Block
  /** If this cell should be hidden from view (ie, for the 'vanish zone' on top of the playfield */
  var isHidden: Boolean
  /** Attempts to put a new Block into this Cell. Returns true if successful */
  def put(b: Block): Boolean
  /** Removes the existing block from this cell. */
  def remove
  /** Cascades a shift down through a single column. */
  def shiftDown: Block
  /** Cascades a shift up through a single column. */
  def shiftUp(b:Block): Unit
  def getNeighbor(d : Direction.Value): Cell
  def getNeighbors = Direction.map(getNeighbor(_)).toList
  /** Returns True if this Cell borders empty space in the specified direction. */
  def isBorder(d:Direction.Value): Boolean = !isEmpty && getNeighbor(d).isEmpty
  def isEmpty = {block.blockType == BlockType.Empty}
  /** A Cell is "open" if the currently active Piece can be moved into it. */
  def isOpen = {isEmpty || block.isActive}
}

trait CellView extends View {
  def blockPut(b: Block)
  def blockRemove(b: Block)
}

/** A single block. contains the block type and any other random state. */
trait BlockModel extends Model[BlockView]
trait BlockView extends View



/** Abstract Model / View publish/subscribe pattern */

// Type parameter upper bounds(<:) - A must be a subtype of View
trait Model[A <: View] {
  // Register/unregister the view listeners on a List
  var views: List[A] = Nil
  def register(view : A) = { views ::= view }
  def unregister(view : A) = { views -= view }

  // Sends the provided function to all registered listeners
  def notifyView(f: A => Unit) =  views.map(v => f(v))
  // Empty 'ping' method invocation
  def notifyView() : Unit = notifyView(_.vnotify(this))
}

trait View {
  // Simple 'ping' notification included by default, can be overridden to perform some operation
  def vnotify[A <: View](model : Model[A]) = println("View "+this+" notified by "+model+"!")
} 