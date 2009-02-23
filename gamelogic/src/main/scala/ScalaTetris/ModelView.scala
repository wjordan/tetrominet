package ScalaTetris

import mode.PlayMode

/**
 * Contains the primary model/view patterns and the derived interfaces.
 * @author will
 * @date Oct 5, 2008 6:51:13 AM
 */

/**Abstract Model-View publish/subscribe pattern template*/

// Type parameter upper bounds(<:) = "A must be a subtype of View"
trait Model[A <: View] {
  // Register/unregister the view listeners on a List
  var views: List[A] = Nil

  def register(view: A) = {views ::= view}

  def unregister(view: A) = {views -= view}

  /**Send the provided function to all registered listeners*/
  def notifyView(f: A => Unit) = views.map(v => f(v))

  /**Empty 'ping' method invocation*/
  def notifyView(): Unit = notifyView(_.vnotify(this))
}

trait View {
  /**Simple 'ping' notification included by default, can be overridden to perform some operation*/
  def vnotify[A <: View](model: Model[A]) = println("View " + this + " notified by " + model + "!")
}

/**Model of the Tetrion */
trait TetrionModel extends Model[TetrionView] {
  /**The core play field model is a 2D Array of Cells*/
  def model: Array[Array[Cell]]

  /**All currently queued Tetrominoes*/
  var pieceQueue: PieceQueue

  /**The currently active tetromino Piece*/
  var piece: Piece

  /**The current lock counter on the active piece*/
  var lockCounter: Int

  /**Current gravity counter (unit fraction)*/
  var gravityCounter: Double

  /**Cell-Map of all of the blocks currently in the playfield*/
  val blockMap: scala.collection.mutable.Map[Block, Cell]

  /**The gameplay mode of this Tetrion*/
  val playmode: PlayMode
}

trait TetrionView extends View {
  /**Called once per cell.*/
  def notifyCellCreate(c: Cell)

  /**Called when a new Piece is created.*/
  def pieceCreate(p: Piece)

  /**Called when the active Piece is locked to the playfield.*/
  def pieceLock(p: Piece)

  /**Called once for each cleared line.*/
  def lineClear(b: Array[Cell])

  def updateState

  def lineClearEnd
}

/**A single unit position within the Tetrion that may hold a single block.*/
trait Cell extends Model[CellView] {
  /**Fixed position index of this cell within the Tetrion*/
  val pos: Pos

  /**Every Cell contains an empty Block by default*/
  val emptyBlock: Block

  /**Current Block contained within this Cell, if any*/
  var block: Block

  /**If this cell should be hidden from view
   * (ie, for the 'vanish zone' on top of the playfield*/
  var isHidden: Boolean

  /**Attempt to put a new Block into this Cell.
   * @return true if successful*/
  def put(b: Block): Boolean

  /**Remove the existing block from this Cell.*/
  def remove

  /**Cascade a shift down through a single column.
   * @return the Block removed by the shift.*/
  def shiftDown: Block

  /**Cascades a shift up through a single column.
   * @param b the Block to insert in the bottom Cell.*/
  def shiftUp(b: Block): Unit

  /**
   * @return Adjacent Cell in the specified Direction.*/
  def getNeighbor(d: Direction.Value): Cell

  /**
   * @return List of neighboring Cells in all Directions.*/
  def getNeighbors = Direction.map(getNeighbor(_)).toList

  /**
   * @return true if this Cell borders empty space in the specified direction. */
  def isBorder(d: Direction.Value): Boolean = !isEmpty && getNeighbor(d).isEmpty

  /**A Cell is "open" if the currently active Piece can be moved into it. */
  def isOpen = {isEmpty || block.isActive}

  def isEmpty = {block.blockType == BlockType.Empty}
}

trait CellView extends View {
  /**A new Block has been put into this Cell.*/
  def blockPut(b: Block)
  /** The existing Block has been removed from this Cell.*/
  def blockRemove(b: Block)
}

/**A single block. Contains the block type and any other state. */
trait BlockModel extends Model[BlockView]
trait BlockView extends View
