/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

package Tetromi.model


import scala.collection.mutable.{SynchronizedMap, HashMap}
import scala.collection.mutable
import Tetromi.net.{BattleEffect, AddLinesEffect}
import Tetromi._

/**Defines a single state. */
trait GState {
  /**Executed once per frame. */
  def run: Unit = {}

  /**Called when the state is first entered
   * @return the number of frames before the state expires */
  def enter: Int = -1

  /**Called when the state's counter expires.
   * @return the new State to enter. */
  def exit: GState = this
}

/**
 * The finite state machine implementing the core Tetrion game logic.
 * @author will
 * @date Oct 23, 2008 4:23:52 AM
 */
trait TetrionState {
  this: Tetrion =>
  var state: GState = this.Spawn
  var control: State = EmptyState

  var lineAdd = 0
  var frame = 0

  val battleEffects = mutable.Set.empty[BattleEffect]

  def get: GState = state

  /**Enters a new game state, updating the timer counter.
   * @return the updated timer counter (so state redirects can be chained) */
  def set(s: GState): Int = {
    state = s;
    counter = state.enter;
    notifyView(_.updateState)
    counter
  }

  /**Executes a single frame of the state machine.*/
  def run(c: State): Unit = {
    control = c
    if (state == GameOver) return
    if (control.option == Option.Empty) return

    if (control.option == Option.End) set(GameOver)
    if (control.option == Option.AddLine) lineAdd += 1
    battleEffects.foreach(_.run)
    incDAS(control.movement)
    counter -= 1
    if (counter == 0) set(state.exit)
    else state.run

    // Increase total frame counter at the end of the frame
    frame += 1
  }

  var rotateVal: Rotation.Value = Rotation.None

  /**This simple logic causes rotations to occur only once per unique button input. */
  def rotated(rot: Rotation.Value): Boolean =
    if (rotateVal == rot) false else {rotateVal = rot; true}

  object Falling extends GState {
    override def enter = {rotateVal = control.rotation; -1}

    override def run = {
      // 1. Rotation / Wall Kicks
      if (rotated(control.rotation)) rotate(rotateVal)
      // 2. Check for "block out"
      if (isBlockOut) set(GameOver)
      // 3. Lateral Movement
      doMovement(control.movement)
      // 4. Gravity / Line clears
      doGravity(control.drop)
    }
  }

  object Paused extends GState

  /**Are is the delay in between piece lock and next piece spawn */
  object Are extends GState {
    // Activate any battle effects in the queue
    override def enter = {
      if (!battleQueue.isEmpty) {
        val effect = battleQueue.dequeue
        battleEffects += effect
        effect.set(TetrionState.this)
      }
      else if (lineAdd > 0) set(LineAdd)
      else playmode.are
    }

    override def exit = Spawn
  }

  object GameOver extends GState {
    override def enter = {println("Game OVER!"); -1}
  }

  object Spawn extends GState {
    override def enter =
      if (!setNewPiece(control.rotation)) set(GameOver)
      else 1

    override def exit = Falling
  }

  object LineClear extends GState {
    override def enter = {
      playmode.pieceLock
      if (checkLineClears) {
        // Send battle effect on 2 or more lines cleared
        if (clearedBlocks.length >= 2) battleController ! (BattleMessage(new AddLinesEffect(clearedBlocks), TetrionState.this))
        playmode.lineClear(clearedBlocks.length)
        // Extra delay is added when line(s) are cleared
        playmode.lineClearDelay
      } else set(Are)
    }

    override def exit = {
      for (lineClear <- clearedLines) lineClear.map(_.shiftDown)
      notifyView(_.lineClearEnd)
      Are
    }
  }

  /**Adds opponent's cleared lines to the playfield.*/
  object LineAdd extends GState {
    override def enter = 10

    override def exit = {
      for (i <- 0 until lineAdd) bottomRow.map(_.shiftUp(InvalidCell.block))
      notifyView(_.lineClearEnd)
      lineAdd = 0
      Are
    }
  }


  var counter: Int = state.enter
}