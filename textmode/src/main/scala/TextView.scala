import charva.awt._
import charva.awt.event._
import charvax.swing.JFrame
import charvax.swing.JPanel

import scala.actors.Actor
import scala.actors.Actor._

import scala.collection.mutable.Map
import ScalaTetris._
import ScalaTetris.local.SinglePlayerGame

/**
 * Text-mode view implementation using Charva Curses bindings (requires JNI library).
 * @author will
 * @date Oct 26, 2008 7:55:10 PM
 */

class TextView extends Container with TetrionView {
  var tetrion : TetrionModel = null
  val controller = new TextControl

  def updateState = {}

  val cellViewMap:Map[Cell,TextCellView] = Map.empty[Cell,TextCellView]

  def getView(c : Cell) = cellViewMap.getOrElse(c, new TextCellView(InvalidCell))

  def pieceLock(p: Piece) = {
    for(block:Block <- p.blocks) {
      val cell: Cell = tetrion.blockMap(block)
      val cv = getView(cell)
      block.isActive = false
      cv.draw
    }
  }

  /** Triggered when the model creates a new piece. */
  def pieceCreate(piece : Piece) = {
    for((block,pos) <- piece.blockSet) {
      val cell = tetrion.model(pos._1.toInt+3)(pos._2.toInt)
      val view:TextCellView = getView(cell)
      view.blockRemove(block)
    }

    val nextPiece = tetrion.pieceQueue.apply(0)
    for((block,pos) <- nextPiece.blockSet) {
      val nextCell = tetrion.model(pos._1.toInt+3)(pos._2.toInt)
      val view = getView(nextCell)
      view.blockPut(block)
    }

    // TODO: Shadow blocks for the active Piece.
  }

  def lineClearEnd = {}

  def lineClear(b: Array[Cell]) = {}

  def notifyCellCreate(cell : Cell) = {
    val view = new TextCellView(cell)
    cellViewMap(cell) = view
    add(view)
  }
  
  def addTetrion(t:TetrionModel) = {
    tetrion = t
    for(row <- tetrion.model; cell <- row)
      notifyCellCreate(cell)

    pieceCreate(tetrion.piece)

    // Listen to the Tetrion model
    tetrion.register(this)
  }


  override def getSize = new Dimension(20, 20)

  override def getWidth = 20

  override def getHeight = 20

  override def minimumSize = new Dimension(22, 25)

  override def debug(i:Int) = {}


  override def processKeyEvent(p1: KeyEvent): Unit = {
    controller.keyPressed(p1)
  }
}

/** Spatializes a cell by managing its enclosed Block Sprite. */
class TextCellView(c : Cell) extends Component with CellView {
  var pos = c.pos

  var block = c.emptyBlock

  // Listen to the Cell model
  c.register(this)
  blockPut(c.emptyBlock)
  blockPut(c.block)

  def draw = {
    val point: Point = new Point((pos.x*2).toInt+2,pos.y.toInt+2)
    Toolkit.getDefaultToolkit().setCursor(point);
    TextBlockView.draw(block)
  }

  def getSize = new Dimension(2, 1)

  def getWidth = 2

  def getHeight = 1

  def minimumSize = new Dimension(2, 1)

  def debug(i: Int) = {}

  /** A new Block is put into this Cell. */
  def blockPut(b: Block) = {

    block = b; draw
  }

  def blockRemove(b: Block) = { block = c.emptyBlock; draw }
  override def toString="cell:"+pos

}

/** Simple block visualization using a Pulpcore FilledSprite. */
object TextBlockView {
  import BlockType._
  import charva.awt.Color._

  val tk: Toolkit = Toolkit.getDefaultToolkit

  def draw(b: Block) = {
    val color = blockColor(b)
    val colorPair = tk.getColorPairIndex(new ColorPair(
      if(b.isActive)color else black,
      if (b.blockType != Empty) color else black))
    val attrib = if (b.isActive) Toolkit.A_BOLD else Toolkit.A_DIM
    val string = {
      if(b.isActive) "▓▓"
      else if(b.blockType != Empty) "  "
      else "░░"
    }

    tk.addString(string, attrib, colorPair);
    tk.setCursor(0,0)
  }
  def blockColor(b: Block) = {
    b.blockType match {
      case Empty => Color.black
      case Garbage => white
      case I => red
      case T => cyan
      case L => white
      case O => yellow
      case J => blue
      case S => magenta
      case Z => green
      case _ => black
    }
  }
}

object TextViewApp extends JFrame {
  _insets = new Insets(1, 1, 1, 1);
  this.setTitle("TetromiNET")

  this.addKeyListener(new KeyAdapter(){
    override def keyPressed(ev: KeyEvent) = {
      if(ev.getKeyCode == 'q') System.exit(0)
    }
  })

  System.setProperty("charva.color", "")
  val tk: Toolkit = Toolkit.getDefaultToolkit();
  val view = new TextView
  val control: Controller = view.controller
  val tetrion = new Tetrion(control, new SinglePlayerGame(control))

  val contentPane = this.getContentPane
  contentPane.setLayout(new BorderLayout);

  contentPane.add(view);
  tk.startColors
  view.addTetrion(tetrion)
  tetrion.update

  this.pack
  this.setVisible(true)

  /** Schedules controller updates to fire at fixed intervals. */
  val TimerActor = new Actor { def act() = loop{
    tetrion.update
    Thread.sleep(20)
  }}

  TimerActor.start

  /** Application entry point. */
  def main(args: Array[String]) {
  }

}
