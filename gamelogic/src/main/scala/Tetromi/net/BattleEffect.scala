/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

package Tetromi.net

import scala.xml._

/**
 * @author will
 * @date Oct 11, 2008 1:56:54 PM
 */

trait BattleEffect {
  /** Reference to the Tetrion the effect controls. */
  var tetrion: Tetrion = null
  /** Length in frames that this effect lasts. This is the number of times the run method will be called. */
  val duration: Int

  var frame: Int = 0

  /** Executes the battle effect. */
  def run = {
    frame += 1
    if(frame >= duration) { exit; tetrion.battleEffects -= this }
    else update
  }

  /** Called each intermediate frame. */
  protected def update: Unit = {}

  /** Called when the effect begins. */
  protected def enter: Unit = {
    tetrion.set(tetrion.Paused)
  }

  def set(t: Tetrion): Int = { println(t+":BATTLE EFFECT set!")
    tetrion = t; enter; duration }

  /** Called when the effect finishes. */
  protected def exit: Unit = {
    tetrion.set(tetrion.Are)
  }

  def toXml: NodeSeq = effectXml
  def effectXml: NodeSeq
}

class AddLinesEffect(garbageRows: Seq[Array[Block]]) extends BattleEffect {
  val duration = 20
  override def enter = { super.enter
    for(garbageRow <- garbageRows) {
      garbageRow(0) = InvalidCell.block
      List.map2(tetrion.bottomRow.toList, garbageRow.toList)(
      (x,y) => { x.shiftUp(y) }
      )
    }
    tetrion.notifyView(_.lineClearEnd)
//    for(garbageRow <- garbageRows) tetrion.bottomRow.map(_.shiftUp(InvalidCell.block))
  }
  def effectXml: NodeSeq = for(garbageRow <- garbageRows) yield {<AddLine blocks={Block.toXmlString(garbageRow)}/>}
}
object BattleEffect {
  def fromXml(nodeRoot: Node): BattleEffect = {
    val node = nodeRoot.child
    val addLines = for(line @ <AddLine>{_*}</AddLine> <- node) yield {Block.fromXmlString((line \ "@blocks").text)}
    new AddLinesEffect(addLines)
  }
}
