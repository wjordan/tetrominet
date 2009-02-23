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

/** A single block. Contains the block type and any other state. */
class Block (b: BlockType.Value) extends BlockModel {
  val blockType = b
  var rotation = PieceRotation.Zero
  /** If this block is a part of the currently active Piece (and shouldn't be included in collision checking) */
  var isActive = false
  override def toString =  "Block:"+blockType

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
