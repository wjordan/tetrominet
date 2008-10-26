package ScalaTetris

import mode.PlayMode

/**
 * Contains the primary model interfaces.
 * @author will
 * @date Oct 5, 2008 6:51:13 AM
 */

/** Model of the Tetrion */
trait TetrionModel extends Model[TetrionView] {
  /** The core play field model is a 2D Array of Cells */
  def model : Array[Array[Cell]]
  /** All currently queued Tetrominoes */
  var pieceQueue: PieceQueue
  /** The currently active tetromino Piece */
  var piece: Piece
  /** The current lock counter on the active piece */
  var lockCounter: Int
  /** Current gravity counter (unit fraction) */
  var gravityCounter : Double
  /** Cell-Map of all of the blocks currently in the playfield */
  val blockMap : scala.collection.mutable.Map[Block,Cell]
  
  val playmode: PlayMode
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
  /** Every Cell contains an empty Block by default. */
  val emptyBlock : Block
  /** The Current Block contained within this Cell, if any */
  var block : Block
  /** If this cell should be hidden from view (ie, for the 'vanish zone' on top of the playfield */
  var isHidden: Boolean
  /** Attempt to put a new Block into this Cell. Returns true if successful */
  def put(b: Block): Boolean
  /** Remove an existing block from this Cell. */
  def remove
  /** Cascade a shift down through a single column. */
  def shiftDown: Block
  /** Cascades a shift up through a single column. */
  def shiftUp(b:Block): Unit

  def getNeighbor(d : Direction.Value): Cell
  def getNeighbors = Direction.map(getNeighbor(_)).toList

  /** Return true if this Cell borders empty space in the specified direction. */
  def isBorder(d:Direction.Value): Boolean = !isEmpty && getNeighbor(d).isEmpty

  /** A Cell is "open" if the currently active Piece can be moved into it. */
  def isOpen = {isEmpty || block.isActive}
  def isEmpty = {block.blockType == BlockType.Empty}
}

trait CellView extends View {
  def blockPut(b: Block)
  def blockRemove(b: Block)
}

/** A single block. Contains the block type and any other random state. */
trait BlockModel extends Model[BlockView]
trait BlockView extends View

/** Abstract Model-View publish/subscribe pattern tepmlate */

// Type parameter upper bounds(<:) = "A must be a subtype of View"
trait Model[A <: View] {
  // Register/unregister the view listeners on a List
  var views: List[A] = Nil
  def register(view : A) = { views ::= view }
  def unregister(view : A) = { views -= view }

  // Send the provided function to all registered listeners
  def notifyView(f: A => Unit) =  views.map(v => f(v))
  // Empty 'ping' method invocation
  def notifyView() : Unit = notifyView(_.vnotify(this))
}

trait View {
  // Simple 'ping' notification included by default, can be overridden to perform some operation
  def vnotify[A <: View](model : Model[A]) = println("View "+this+" notified by "+model+"!")
} 