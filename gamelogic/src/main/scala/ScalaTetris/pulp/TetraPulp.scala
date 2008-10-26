package ScalaTetris.pulp

import pulpcore.animation.event.TimelineEvent
import pulpcore.animation.{Timeline, Easing, Color}
import pulpcore.scene.Scene2D
import pulpcore.Stage
import pulpcore.sprite._
import pulpcore.image.Colors
import ScalaTetris._
import pulpcore.scala.PulpCore._
import scala.collection.mutable.Map
import Pos._
import pulpcore.Input

/**
 * Contains all of the Pulpcore Views for TetraScala.
 * @author will
 * @date Oct 4, 2008 8:32:31 AM
 */

/** Pulpcore View object for the Tetrion, using a PulpCore Group. */
class PulpTetrionView (t : TetrionModel, blockSize : Int) extends Group with TetrionView {
  var bSize = blockSize // Scale each block this many pixels
  val tetrion = t
  val blockViewMap = Map.empty[Block,PulpBlockView]
  val cellViewMap = Map.empty[Cell,PulpCellView]

  val hiddenGroup: Group = new Group
  val blockGroup: Group = new Group
  val emptyGroup: Group = new Group
  add(emptyGroup); add(blockGroup); add(hiddenGroup)

  def addBlockView(view : PulpBlockView) = { blockViewMap(view.block) = view; view }
  def addCellView(view : PulpCellView) = { cellViewMap(view.cell) = view; add(view); view }

  def getBlockView(b : Block) = {
    val blockView = blockViewMap.getOrElseUpdate(b,new PulpBlockView(b))
    if(!blockView.active) {
      blockView.active = true;
      if(b.blockType == BlockType.Empty) emptyGroup.add(blockView)
      else blockGroup.add(blockView)
    }
    blockView
  }
  def getCellView(c : Cell) = cellViewMap.getOrElseUpdate(c, new PulpCellView(InvalidCell,this))

  def getView(b: Block) = {
    if(blockViewMap.contains(b)) blockViewMap(b) else PulpBlockView.Empty
  }

  def removeBlockView(b : Block) = {
    val view = getBlockView(b)
    blockViewMap - b
    blockGroup.remove(view)
    view.active = false
  }

  // Initialize view by adding all of the (empty) cells in the current model
  for(row <- tetrion.model; cell <- row)
    notifyCellCreate(cell)

  // Listen to the Tetrion model
  tetrion.register(this)

  def notifyCellCreate(cell : Cell) = {
    addCellView(new PulpCellView(cell, this))
  }

  var currentPiece: Piece = EmptyPiece
  var nextPiece : Piece = EmptyPiece

  def pieceLock(piece : Piece) = {
    // For a piece lock, flash the piece white and then return to its original color slightly dimmed.
    for(block <- piece.blocks) {
      val bv = getView(block)
      bv.fillColor.animateTo(Colors.WHITE,50)

      // Update borders for all blocks touching the locked Piece.
      val c : Cell = t.blockMap(block)
      getCellView(c).setBorders
      c.getNeighbors.foreach(getCellView(_).setBorders)
//      setAllBorders

      // Make sure this block's animated values are fixed once the piece locks
      scene.addEvent(new TimelineEvent(50) {
        def run = {
          bv.fillColor.animateTo(Colors.rgba(bv.blockColor,100),50)
//          bv.fillColor.animateTo(Colors.BLACK,50)
          bv.angle.stopAnimation(true)
          bv.x.stopAnimation(true)
          bv.y.stopAnimation(true)
        }
      })
    }
  }

  def lineClear(a : Array[Cell]) = { setAllBorders }
  def lineClearEnd = { setAllBorders }
  def setAllBorders = tetrion.model.map(_.map(getCellView(_).setBorders))

  def getAllCells(v : PulpBlockView) = tetrion.model.flatMap(_.filter(getCellView(_).blockView == v))
  def updateState = {}

  /** Triggered when the model creates a new piece. */
  def pieceCreate(piece : Piece) = {
//    for(block <- currentPiece.blocks) getView(block).setAlpha(100)
    currentPiece = piece

    // Add the "next" piece to the top of the frame
    nextPiece = tetrion.pieceQueue.apply(0)
    for((block,pos) <- nextPiece.blockSet) {
      val nextCell = tetrion.model(pos._1.toInt+3)(pos._2.toInt)
      getCellView(nextCell).blockPut(block)
    }

    // TODO: Shadow blocks for the active Piece.
  }

  def scene: Scene2D = Stage.getScene.asInstanceOf[Scene2D]

  val scoreLabel = new Label("Score: ",300,300)
  add(scoreLabel)


  override def update(elapsedTime: Int): Unit = {
    super.update(elapsedTime)
    scoreLabel.setText("Score: "+tetrion.playmode.score)
  }
}


/** Spatializes a cell by managing its enclosed Block Sprite. */
class PulpCellView(c : Cell, tv : PulpTetrionView) extends Group with CellView {
  val cell = c
  var pos = cell.pos * tv.bSize
  var blockView : PulpBlockView = PulpBlockView.Empty

  // Listen to the Cell model
  c.register(this)
  blockPut(c.emptyBlock)
  blockPut(c.block)

  def setBorders = {
    val map = Map.empty[Direction.Dir,Int]
    for (dir <- Direction.elements) {
      val d = dir.asInstanceOf[Direction.Dir]
      map += (d -> (if (cell.isBorder(d)) 2 else 0))
    }
    val b = cell.block
    blockView.setBorderSize(
      map(Direction.North + b.rotation),
      map(Direction.West + b.rotation),
      map(Direction.South + b.rotation),
      map(Direction.East + b.rotation))
  }

  /** Darken the cells at the top of the screen using overlaid gray alpha sprites. */
  if(c.isHidden) {
    val darkSprite: FilledSprite = new FilledSprite(pos.x,pos.y,tv.bSize,tv.bSize,Colors.gray(0,100))
    darkSprite.setAnchor(Sprite.CENTER)
    tv.hiddenGroup.add(darkSprite)
  }

  /** Triggered when a new Block is put into this Cell. */
  def blockPut(b: Block) = {
    // Put this Block's sprite into this CellView.
    blockView = tv.getBlockView(b)
    blockView.setSize(tv.bSize)
    val moveSpeed = 100
    blockView.setLocation(pos.x,pos.y)

    // Ensure the block spins in the nearest direction
    val rotation = PieceRotation(b.rotation)
    while(blockView.angle.get - rotation > Math.Pi) blockView.angle.set(blockView.angle.get - (Math.Pi*2))
    while(blockView.angle.get - rotation < -Math.Pi ) blockView.angle.set(blockView.angle.get + (Math.Pi*2))
    blockView.angle.set(rotation)
  }

  def blockRemove(b: Block) = {
    tv.removeBlockView(b)
  }

  override def update(elapsedTime: Int) = {
    super.update(elapsedTime)

/*
    // Lower the block smoothly with gravity
    if(c.block.isActive) {
      val gravityPos = Pos(pos.x,(cell.pos.y + Math.min(1,tv.tetrion.gravityCounter)) * tv.bSize)
      blockView.y set gravityPos.y
    }
    else pos = Pos(pos.x,cell.pos.y * tv.bSize)
*/

    // Dim the active piece based on its floor lock counter
    if(c.block.isActive) blockView.setAlpha(255 - (tv.tetrion.lockCounter*5))
  }
}

/** Simple block visualization using a Pulpcore FilledSprite. */
class PulpBlockView (b : Block) extends FilledSprite(Colors.WHITE) with BlockView {
  this.pixelSnapping.set(true)
  val block: Block = b
  var active : Boolean = false
  setAnchor(Sprite.CENTER)
  // Set the sprite's color based on the block type
  fillColor.set(if(b.isActive) blockColor else blockColor)
  borderColor.set(Colors.WHITE)

  def blockColor = block.blockType match {
    case BlockType.Empty => Colors.DARKGRAY
    case BlockType.Garbage => Colors.GRAY
    case BlockType.I => Colors.RED
    case BlockType.T => Colors.CYAN
    case BlockType.L => Colors.ORANGE
    case BlockType.O => Colors.YELLOW
    case BlockType.J => Colors.BLUE
    case BlockType.S => Colors.PURPLE
    case BlockType.Z => Colors.GREEN
    case _ => Colors.WHITE
  }
  def setLocation(pos : Pos) : Unit = setLocation(pos.x,pos.y)
  def setSize(size : Double) : Unit = setSize(size,size)


  def setAlpha(alpha:Int) = fillColor.set(Colors.rgba(blockColor,alpha))
  override def toString = "PulpBlockView, location="+x+","+y+",size="+width+","+height+",objectref="+this.hashCode
}
object PulpBlockView {
  val Empty: PulpBlockView  = new PulpBlockView(new Block(BlockType.Empty))
}