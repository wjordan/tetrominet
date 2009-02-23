/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

import charva.awt._
import charva.awt.event._
import charvax.swing.JFrame

import charvax.swing.JLabel
import charvax.swing.JPanel


import charvax.swing.JTextArea
import scala.actors.Actor
import scala.actors.Actor._

import scala.collection.mutable.Map
import Tetromi._
import Tetromi.local.SinglePlayerGame
import mode.Playmode

import Tetromi.net.BattleController
import Tetromi.net.Client
import Tetromi.net.ClientView
import Tetromi.net.BattleLocal

/**
 * Text-mode view implementation using Charva Curses bindings (requires link to JNI library).
 * @author will
 * @date Oct 26, 2008 7:55:10 PM
 */

class TextView extends Container with TetrionView {
  var tetrion : TetrionModel = null
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
      val cell = tetrion.model(pos._1.toInt+3)(pos._2.toInt-1)
      val view:TextCellView = getView(cell)
      view.blockRemove(block)
    }

    val nextPiece = tetrion.pieceQueue.apply(0)
    for((block,pos) <- nextPiece.blockSet) {
      val nextCell = tetrion.model(pos._1.toInt+3)(pos._2.toInt-1)
      val view = getView(nextCell)
      view.blockPut(block)
    }

    // TODO: Shadow blocks for the active Piece.
  }

  def lineClearEnd = {}

  def lineClear(b: Array[Cell]) = {}

  def notifyCellCreate(cell : Cell) = {
    val view = new TextCellView(cell)
    view.setLocation((cell.pos.x*2+1).toInt, cell.pos.y.toInt)
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


  override def minimumSize = new Dimension(22, 24)

  override def debug(i:Int) = {}

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
    Toolkit.getDefaultToolkit().setCursor(getLocationOnScreen());
    TextBlockView.draw(block)
  }

  def getSize = new Dimension(2, 1)

  def getWidth = 2

  def getHeight = 1

  def minimumSize = new Dimension(2, 1)

  def debug(i: Int) = {}

  /** A new Block is put into this Cell. */
  def blockPut(b: Block) = {
    block = b; repaint
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
      if(b.isActive)color else black, // foreground
      if (b.blockType != Empty) color else black)) // background
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

  var game: BattleController = null
  var controller: TextControl = new TextControl
  val player1: TextView = new TextView
  val player2: TextView = new TextView
  _insets = new Insets(1, 1, 1, 1);
  setTitle("TetromiNET")

  this.addKeyListener(new KeyAdapter(){
    override def keyPressed(ev: KeyEvent) = {
      if(ev.getKeyCode == 'q') System.exit(0)
      controller.keyPressed(ev)
    }
  })

  val client:Client  = new Client(new ClientView{
    def gameOver(iWin:Boolean): Unit = { println("Game over! You "+
            (if(iWin)"WIN!" else "LOSE!") )}
    def gameStart(seed: Long): Unit = {
      label.setText("")
      controller = new TextControl(seed)
      val mGame = new BattleLocal(controller)
      game = mGame
      player1.addTetrion(mGame.players(0))
      contentPane.add(player2)
      player2.setLocation(30,2)
      player2.addTetrion(mGame.players(1))
      TimerActor.start
      client.startGame(mGame)
    }
  })

  System.setProperty("charva.color", "")
  Toolkit.getDefaultToolkit.startColors

  def startSingleGame = {
    game = new SinglePlayerGame(controller,Playmode.Easy)
    val tetrion = game.players(0)
    val view = new TextView
    view.validate
    view.addTetrion(tetrion)
    contentPane.add(view);
    TimerActor.start
  }

  val contentPane = this.getContentPane
  contentPane.setLayout(new BorderLayout);

  val label = new JTextArea()
  label.setEditable(false)
  contentPane.add(label)
  contentPane.add(player1)
  this.pack
  this.setVisible(true)
  label.validate
  label.setLocation(30,2)
  label.setText("Waiting for opponent...\n" +
          "WASD keys to move, [] keys to rotate\n" +
          "Or arrow keys to move, ZX keys to rotate")

  /** Schedules controller updates to fire at fixed intervals. */
  val TimerActor = new Actor { def act() = loop{
    game.update
    Thread.sleep(20)
  }}

  /** Application entry point. */
  def main(args: Array[String]) {
  }

}
