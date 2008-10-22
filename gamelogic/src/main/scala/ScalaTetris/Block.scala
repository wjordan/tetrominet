package ScalaTetris

import net.{BattleEffect, BattleController, AddLinesEffect}
import pulp.PulpControl
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable
import Pos._
import scala.collection.mutable.{SynchronizedMap, HashMap, SynchronizedQueue, Map}
import scala.xml.NodeSeq

/*
object Tetrion {
}
*/

/** This object encapsulates the Tetrion's entire game state machine. */
object GameState extends Enumeration("Spawn","Falling","Are","LineClear","LineAdd","Paused","GameOver") {
  val Spawn, Falling, Are, LineClear, LineAdd, Paused, GameOver = Value
}

import GameState._

object DoUpdate

case class BattleMessage(effect: BattleEffect, from: Tetrion)
case class PlayBattleMessage(effect: (BattleEffect,Int), from: PlaybackController)

/** Model containing the playing field. */
class Tetrion(val Control: Controller, val battleController: BattleController) extends TetrionModel {
  override def toString = { (if (this == battleController.player) "PlayerTetrion" else "OpponentTetrion") + ":#"+frame }
  
  val battleQueue = new SynchronizedQueue[BattleEffect];
  val messageQueue = new SynchronizedQueue[Any];

  var pieceQueue : PieceQueue = new PieceQueue(Randomizer.Memoryless,Control.seed)

  /** Gravity expressed in number of rows descended per frame (TGM works in fractions of 256 so we'll follow that convention) */
  var gravity: Double = (8.0 / 256)
  val defaultPos = Pos(3, 3)
  val are = 16
  val lineClearDelay = 12
  val maxLockDelay = 30

  var score = 0

  var piece : Piece = EmptyPiece
  var piecePos : Pos = defaultPos

  implicit def toPos(xy : (Int,Int)) : Pos = new Pos(xy)

  /** Manages battle effect messages sent by the multiplayer battle controller. */
  def !(msg: Any) = { messageQueue += msg }
  
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

 
  /** Sets a new piece on the playfield. Takes into account initial rotation.
      @return false if the piece could not be placed. */
  def setNewPiece(IRS: Rotation.Value): Boolean = {
    piecePos = defaultPos
    piece = pieceQueue()
    notifyView(_.pieceCreate(piece))
    if(rotate(IRS)) true else put(piece,piecePos)
  }

  /** Remove a piece / block from the playfield. */
  def remove(piece : Piece): Unit = for(block <- piece.blocks) remove(block)
  def remove(block : Block): Unit = if(blockMap.contains(block)) blockMap(block).remove

  /** Locks the current piece to the playfield, checking for any line clears. */
  def lockPiece() = {
    lockCounter = 0
    score += 1
    piece.lock
    println(this+": Lock pieces on frame#"+frame)
    notifyView(_.pieceLock(piece))

    set(LineClear)
  }

  /** Checks the playfield for any cleared lines and returns a Sequence of rows with the line numbers. */
  def lineClears = {
    for (i <- 0 until playField(0).length; row = getRow(i); if row.filter(_.isEmpty).length == 0) yield row
  }

  var clearedLines: Seq[Array[Cell]] = Seq.empty
  var clearedBlocks: Seq[Array[Block]] = Seq.empty

  def checkLineClears = {
    clearedLines = lineClears.toList
    clearedBlocks = clearedLines.map(_.map(_.block))
    for (lineClear <- clearedLines) {
      lineClear.map(_.remove)
      notifyView(_.lineClear(lineClear))
    }
//    lineAdd += clearedLines.length
    (clearedLines.length > 0)
  }

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

  /** Checks movement of the piece and returns the last valid Pos of the piece before it can't move any further.
      This works for horizontal and vertical piece movements of any length
      Note: this method doesn't actually perform any movement. */
  def movePos(offset : Pos) : Pos = {
    movePos(piecePos,offset,Pos(
      if(offset.x > 0) 1 else if(offset.x < 0) -1 else 0,
      if(offset.y > 0) 1 else if(offset.y < 0) -1 else 0 ))
  }

  /** Recursive function to move the piece by increments until it can't be placed. */
  def movePos(pos: Pos, targetOffset: Pos, incr: Pos): Pos = {
    if (canPut(piece, pos + incr) && targetOffset != Pos(0, 0)) {
      movePos((pos + incr), targetOffset - incr, incr) }
    else pos
  }

  /** Attempts to move the active piece up to the specified offset.
      @return true if there was actually any movement. */
  def move(offset: Pos): Boolean = {
    val newPos = movePos(offset)
    if (piecePos == newPos) false
    else {
//      println(this+" moved "+offset+" on frame#"+frame)
      piecePos = newPos
      put(piece, newPos)
      true
    }
  }

  /** Maps the current Cell positions of all active Blocks. */
  val blockMap = mutable.Map.empty[Block,Cell]

  /** The collection of all static cells that comprise the playing field. */
  type Array2D[A] = Array[Array[A]]
  val playField: Array2D[Cell] = Array.fromFunction((x, y) => {
    val cell: Cell = new TetrionCell(Pos(x, y),this)
    if(y < 4) cell.isHidden = true
    notifyView(_.notifyCellCreate(cell))
    cell
  })(10, 24)
  def getRow(row:Int): Array[Cell] = Array.fromFunction(playField(_)(row))(playField.length)
  def bottomRow = getRow(23)

  def model = playField

  /** Returns an indexed Cell from the playfield, or InvalidCell if the index is out of the field's bounds. */
  def apply(pos: Pos): Cell = {
    if (pos.y < 0 || pos.x < 0 || pos.x >= playField.length || pos.y >= playField(0).length)
      InvalidCell
    else playField(pos.x.toInt)(pos.y.toInt)
  }


  var lineAdd = 0
  var frame = 0

  val battleEffects = mutable.Set.empty[BattleEffect]
  var control: State = EmptyState
  private var state = Spawn
  private var counter: Int = enter

  val stateMap = new HashMap[Int,GameState.Value] with SynchronizedMap[Int,GameState.Value]
  val piecePosMap = new HashMap[Int,Pos] with SynchronizedMap[Int,Pos]
  val controlMap = new HashMap[Int,State] with SynchronizedMap[Int,State]

  def get: GameState.Value = state

  val debug = false

  def run: Unit = {

    if(debug) {
      // Simple synchronization unit tests
      stateMap + (frame -> state)
      if(this == battleController.opponent) assert(stateMap(frame) == battleController.player.stateMap(frame), "statemap "+this+": "+stateMap(frame)+" !="+battleController.player.stateMap(frame))
      piecePosMap + (frame -> piecePos)
      if(this == battleController.opponent) assert(piecePosMap(frame) == battleController.player.piecePosMap(frame), "piecePosMap "+this+": "+piecePosMap(frame)+" !="+battleController.player.piecePosMap(frame))
    }

    control = (Control.poll).asInstanceOf[State]
    if (state == GameOver) return
    if (control.option == Option.Empty) { return} //println("empty!");

    //    if(control != NoneState) println(this+"#"+frame+": control="+control)
    if(debug) {
      controlMap + (frame -> control)
      if(this == battleController.opponent) assert(controlMap(frame) == battleController.player.controlMap(frame), this+": "+controlMap(frame)+" !="+battleController.player.controlMap(frame))
    }

    if (control.option == Option.End) set(GameOver)
    if (control.option == Option.AddLine) lineAdd += 1
    battleEffects.foreach(_.run)
    incDAS(control.movement)
    counter -= 1
    if (counter == 0) exitState


    state match {
      case Falling =>
        rotate(control.rotation) // 1. Rotation / Wall Kicks
        if (isBlockOut) set(GameOver) // 2. Check "block out"
        doMovement(control.movement) // 3. Lateral Movement
        doGravity(control.drop) // 4. Gravity / Line clears
      case _ =>
    }

    // Increase frame counter at the end of the frame
    frame += 1
  }

  /** Called when the state is first entered. */
  def enter: Int = {
    state match {
      case Spawn => if (!setNewPiece(control.IRS)) {
        println("Game over!!")
        set(GameOver)
      } else 1
      case LineClear =>
        if (checkLineClears) {
          if(clearedBlocks.length >= 2) battleController ! (BattleMessage(new AddLinesEffect(clearedBlocks), this))
          lineClearDelay
        }
        else set(Are)
      case Are =>
        // Apply any battle effects in the queue
        if (!battleQueue.isEmpty) {
          println(this+" Adding battle effect")
          val effect = battleQueue.dequeue
          battleEffects += effect
          effect.set(this)
        }
        else if (lineAdd > 0) set(LineAdd)
        else are
      case LineAdd =>
        10
      case _ => -1
    }
  }

  /** Called when the state counter expires. */
  def exitState = state match {
    case LineClear => {
      for (lineClear <- clearedLines) lineClear.map(_.shiftDown)
      notifyView(_.lineClearEnd)
      set(Are)
    }
    case LineAdd => {
      for (i <- 0 until lineAdd) bottomRow.map(_.shiftUp(InvalidCell.block))
      notifyView(_.lineClearEnd)
      lineAdd = 0
      set(Are)
    }
    case Are => set(Spawn)
    case Spawn => set(Falling)
    case _ =>
  }

  /**Enters a new state and updates the timer counter. */
  def set(s: GameState.Value): Int = {
    state = s;
    counter = enter;
    notifyView(_.updateState)
    counter
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
    (lockCounter >= maxLockDelay)
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
    gravityCounter += gravity
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
    if(pieceLocked || (cantMoveDown && drop == Drop.Soft) ) lockPiece
  }

  // Expect this to be called 60 frames per second (matching definition of 'frame' in other Tetris games)
  def update(elapsedTime : Int) = {
    checkMessages
    run
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

class TetrionCell (p: Pos, t: Tetrion) extends Cell {
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
//    println("Shifting up cell="+pos+",block="+b)
    val oldBlock = block
    remove; put(b)
    getNeighbor(Direction.North).shiftUp(oldBlock)
  }

  def remove:Unit = {
    if(block != emptyBlock) {
      t.blockMap - block
      notifyView(_.blockRemove(block))
      block = emptyBlock
    }
  }

  override def toString = "Cell:("+p +","+block+")"
}

/** Default Cell object for invalid cell coordinates */
object InvalidCell extends Cell {
  val pos = Pos(-1,-1)
  def put(b: Block) = false
  var isHidden = true
  var block = new Block(BlockType.Empty)
  def remove = {}
  def shiftUp(b: Block) = {}
  def shiftDown = block
  override def isEmpty = false
  override def getNeighbors = List()
  def getNeighbor(d : Direction.Value): Cell = this
}
