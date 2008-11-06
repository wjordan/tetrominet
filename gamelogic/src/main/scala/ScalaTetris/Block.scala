package ScalaTetris


import mode.{PlayMode, TADeath, EasyMode, Playmode}
import model.TetrionState
import net.{BattleEffect, BattleController, AddLinesEffect}
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable
import Pos._
import scala.collection.mutable.{SynchronizedMap, HashMap, SynchronizedQueue, Map}
import scala.xml.NodeSeq

case class BattleMessage(effect: BattleEffect, from: Tetrion)
case class PlayBattleMessage(effect: (BattleEffect,Int), from: PlaybackController)

/** Implements the core Tetrion playfield data model and manipulation routines. */
abstract class TetrionBase (seed:Long) extends TetrionModel {
  var pieceQueue : PieceQueue = new PieceQueue(Randomizer.Memoryless,seed)

/** Constants */
  /** Initial Starting position of new Pieces */
  val defaultPos = Pos(3, 3)


  var piece : Piece = EmptyPiece
  var piecePos : Pos = defaultPos

  implicit def toPos(xy : (Int,Int)) : Pos = new Pos(xy)


  /** Sets a new piece on the playfield, taking into account initial rotation.
      @return false if the piece could not be placed. */
  def setNewPiece(IRS: Rotation.Value): Boolean = {
    piecePos = defaultPos
    piece = pieceQueue()
    notifyView(_.pieceCreate(piece))
    if(rotate(IRS)) true else put(piece,piecePos)
  }

  /** Removes a piece / block from the playfield. */
  def remove(piece : Piece): Unit = for(block <- piece.blocks) remove(block)
  def remove(block : Block): Unit = if(blockMap.contains(block)) blockMap(block).remove

  /** Locks the current piece to the playfield. */
  def lockPiece = {
    lockCounter = 0
    piece.lock
    notifyView(_.pieceLock(piece))
  }

  /** Returns a Seq of all Block-filled rows. */
  def lineClears = {
    for (i <- 0 until playField(0).length; row = getRow(i); if row.filter(_.isEmpty).length == 0) yield row
  }

  var clearedLines: Seq[Array[Cell]] = Seq.empty
  var clearedBlocks: Seq[Array[Block]] = Seq.empty

  /** Checks for cleared lines, removing the Blocks from their Cells.
   *  @return true if one or more lines were cleared. */
  def checkLineClears = {
    clearedLines = lineClears.toList
    clearedBlocks = clearedLines.map(_.map(_.block))
    for (lineClear <- clearedLines) {
      lineClear.map(_.remove)
      notifyView(_.lineClear(lineClear))
    }
    (clearedLines.length > 0)
  }

  /** Inserts a Piece / Block into the playfield at the specified position.
   *  @return false if the Piece was unable to be placed. */
  def put(piece: Piece, pos : Pos): Boolean = {
    remove(piece)
    if (canPut(piece,pos)) {
      for ((block, blockpos) <- piece.blockSet) put(block, pos + blockpos)
      true
    } else false
  }
  def put(block: Block, pos:Pos) = apply(pos).put(block)

  def canPut(piece: Piece, pos: Pos): Boolean = canPut(piece.blockSet, pos)
  private def canPut(blockSet: Array[(Block, (Int,Int))], offset: Pos): Boolean = {
    if(blockSet.length == 0) return false
    (for ((block, blockpos) <- blockSet; if !apply(offset + blockpos).isOpen)
      yield false
        ).isEmpty
  }

  def canRotate(blockSet: Array[(Block,(Int,Int))]) = {
    val rightKick = piecePos + Pos(1,0)
    val leftKick = piecePos + Pos((-1),0)
    if(canPut(blockSet,piecePos)) piecePos
    else if(canPut(blockSet,rightKick)) rightKick
    else if(canPut(blockSet,leftKick)) leftKick
    else Pos(0,0)
  }

  /** Rotates the current Piece. Returns True if the piece was rotated. */
  def rotate(rot : Rotation.Value) : Boolean = {
    if(rot == Rotation.None) return false
    val newBlockSet = piece.checkRotate(rot)
    val newPiecePos = canRotate(newBlockSet)
    if(newPiecePos != Pos(0,0)) {
      piecePos = newPiecePos
      piece.doRotate(rot)
      put(piece,piecePos)
      true
    } else false
  }

  /** Checks possible movement of the active Piece.
   *  Works for horizontal and vertical movements of any amount.
   *  @return the last valid Pos of the Piece before it can't move any further. */
  def movePos(offset : Pos) : Pos = {
    movePos(piecePos,offset,Pos(
      if(offset.x > 0) 1 else if(offset.x < 0) -1 else 0,
      if(offset.y > 0) 1 else if(offset.y < 0) -1 else 0 ))
  }

  /** Recursive function to move the active Piece by increments until it can't be placed. */
  def movePos(pos: Pos, targetOffset: Pos, incr: Pos): Pos = {
    if (canPut(piece, pos + incr) && targetOffset != Pos(0, 0)) {
      movePos((pos + incr), targetOffset - incr, incr) }
    else pos
  }

  /** Attempts to move the active Piece up to the specified offset.
      @return true if the Piece moved any amount. */
  def move(offset: Pos): Boolean = {
    val newPos = movePos(offset)
    if (piecePos == newPos) false
    else {
      piecePos = newPos
      put(piece, newPos)
      true
    }
  }

  /** Maps the current Cell positions of all active Blocks. */
  val blockMap = mutable.Map.empty[Block,Cell]

  val width = 10
  val height = 24
  /** The collection of all static cells that comprise the playing field. */
  type Array2D[A] = Array[Array[A]]
  val playField: Array2D[Cell] = Array.fromFunction((x, y) => {
    val cell: Cell = new TetrionCell(Pos(x, y),this)
    if(y < 4) cell.isHidden = true
    notifyView(_.notifyCellCreate(cell))
    cell
  })(width, height)
  def getRow(row:Int): Array[Cell] = Array.fromFunction(playField(_)(row))(playField.length)
  def bottomRow = getRow(height-1)

  def model = playField

  /** Returns an indexed Cell from the playfield, or InvalidCell if the index is out of the field's bounds. */
  def apply(pos: Pos): Cell = {
    if (pos.y < 0 || pos.x < 0 || pos.x >= playField.length || pos.y >= playField(0).length)
      InvalidCell
    else playField(pos.x.toInt)(pos.y.toInt)
  }
}


/**
 * Model containing the core gameplay logic.
 * @param Control the Controller providing input state.
 * @param battleController the BattleController to communicate any multiplayer BattleEffects to/from.
 */
class Tetrion(val Control: Controller, val battleController: BattleController, mode:Playmode.Value) extends TetrionBase(Control.seed)
        with TetrionState {
  override def toString = frame.toString /*(if (this == battleController.player) "PlayerTetrion" else "OpponentTetrion") + ":#"+*/

  val playmode: PlayMode = Playmode(mode,this)
  
  val battleQueue = new SynchronizedQueue[BattleEffect];
  val messageQueue = new SynchronizedQueue[Any];

  /** Handles battle effect messages sent by the multiplayer battle controller. */
  def !(msg: Any) = { messageQueue += msg }
  /** Battle Effects are only applied at the start of Are. */
  def checkMessages = while(!messageQueue.isEmpty) {
    messageQueue.dequeue match {
      case BattleMessage(effect: BattleEffect, from: Tetrion) =>
        battleQueue += effect
        println("Received battle effect " + effect + " from " + from + " before frame#"+frame+"!")
        Control += effect
      case PlayBattleMessage(effect: (BattleEffect,Int), from: PlaybackController) =>
        battleQueue += effect._1
        println("Received battle effect " + effect + " from controller " + from + " before frame#"+frame+"!")
        if(frame <= effect._2) Control.effects += effect
      case x => println(this+"unknown message "+x)
    }
  }

  def isBlockOut = { piece.pieceType == PieceType.Empty }

  /** Handle horizontal movement, taking the DAS counter into account. */
  def doMovement(movement: Movement.Value) = {
    if(moveDAS) move(Movement(movement)) // DAS counter expired
  }

  var dasCounter: (Movement.Value,Int) = (Movement.None,0)
  def moveDAS: Boolean = (dasCounter._2 <= 0 || dasCounter._2 == 10)
  def incDAS(movement:Movement.Value) = {
    dasCounter = (dasCounter._1, dasCounter._2 - 1)
    if(movement != dasCounter._1) dasCounter = (movement, 10)
  }

  var gravityCounter: Double = 0
  var lockCounter: Int = 0

  def cantMoveDown: Boolean = (movePos(Pos(0, 1)) == piecePos)
  def pieceLocked(): Boolean = {
    if (cantMoveDown) lockCounter += 1
    (lockCounter >= playmode.maxLockDelay)
  }

  /** Handle vertical movement and piece locking. */
  def doGravity(drop : Drop.Value) = {
    // User supplied drop movements
    var dropVal = drop match {
      case Drop.Hard => 20
      case Drop.Soft => 1
      case Drop.None => 0
    }
    // Make the piece fall extra from gravity
    gravityCounter += playmode.gravity
    if(gravityCounter > 1) {
      val gcRound = gravityCounter.toInt
      gravityCounter -= gcRound
      dropVal += gcRound
    }
    // Do the actual vertical movement
    val moved = move(Pos(0,dropVal))
    // Reset the lock counter if there was any vertical movement
    if(moved) lockCounter = 0
    // Lock the piece if it touches the ground on a soft drop
    if(pieceLocked || (cantMoveDown && drop == Drop.Soft) ) { lockPiece; set(LineClear) }
  }

  /**
   * Advances the Tetrion a single frame through its finite state machine.
   * Expect this to be called 60 frames per second (standard definition of 'frame' in most Tetris games)
   */
  def update = {
    checkMessages
    run((Control.poll).asInstanceOf[State])
    playmode.update
  }
}

class Block (b: BlockType.Value) extends BlockModel {
  val blockType = b
  var rotation = PieceRotation.Zero
  /** If this block is a part of the currently active Piece (and shouldn't be included in collision checking) */
  var isActive = false
  override def toString = { "Block:"+blockType }

  /** XML (de)serialization */
  def toXml: NodeSeq = <Block type={blockType.id.toString}/>
  def this(x: NodeSeq) = this(BlockType((x \\ "@type").text.toInt))
  /** Integer (de)serialization */
  def toInt = blockType.id
  def this(x: Int) = this(BlockType(x))
}

object Block {
  def toXmlString(blocks: Seq[Block]): String = {
    val s = new StringBuilder
    blocks.foreach(i => s.append(i.toInt.toString + " "))
    s.toString
  }
  def fromXmlString(intString: String): Array[Block]  = intString.split(" ").map(x => new Block(x.toInt)).toArray
}

class TetrionCell (p: Pos, t: TetrionBase) extends Cell {
  val pos = p
  // Each Cell keeps its own empty placeholder Block
  val emptyBlock = new Block(BlockType.Empty)
  var block = emptyBlock
  var isHidden = false

  def put(b: Block): Boolean = {
    // Refuse attempts to put other empty blocks into this Cell
    if(isEmpty && b.blockType != BlockType.Empty) {
      t.blockMap.getOrElseUpdate(b,InvalidCell).remove
      t.blockMap + (b->this)
      block = b
      notifyView(_.blockPut(b))
      true
    } else false
  }

  def getNeighbor(d : Direction.Value): Cell = t(pos + Direction(d))

  def shiftDown: Block = {
    val oldBlock = block
    remove
    put(getNeighbor(Direction.North).shiftDown)
    oldBlock
  }

  def shiftUp(b : Block) = {
    val oldBlock = block
    remove; put(b)
    getNeighbor(Direction.North).shiftUp(oldBlock)
  }

  def remove:Unit = {
    if(block != emptyBlock) {
      val b = block
      t.blockMap - b
      block = emptyBlock
      notifyView(_.blockRemove(b))
    }
  }

  override def toString = "Cell:("+p +","+block+")"
}

/** Default Cell object for invalid/out of bounds cell coordinates */
object InvalidCell extends Cell {
  val pos = Pos(-1,-1)
  def put(b: Block) = false
  var isHidden = true
  var block = new Block(BlockType.Empty)
  val emptyBlock = block
  def remove = {}
  def shiftUp(b: Block) = {}
  def shiftDown = block
  override def isEmpty = false
  override def getNeighbors = List()
  def getNeighbor(d : Direction.Value): Cell = this
}
