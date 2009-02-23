/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

import charva.awt.event.KeyEvent
import Tetromi.Controller
import Tetromi.State
import charva.awt.event.KeyEvent._
import scala.collection.mutable

import Tetromi._
import Tetromi.Rotation._
import Tetromi.Movement._
import Tetromi.Direction._
import Tetromi.Option._
import Tetromi.Drop._

/**
 * @author will
 * @date Oct 26, 2008 9:22:24 PM
 */

class TextControl(val seed:Long) extends Controller {
  def this() = this(System.currentTimeMillis)

  var (keyLeft: Int, keyRight: Int, keyRotateCCW: Int, keyRotateCW: Int, keyRotate180: Int,
  keySoftDrop: Int, keyHardDrop: Int, keyQuit: Int, keyLeft2: Int, keyRight2: Int, keySoftDrop2: Int,
  keyHardDrop2: Int, keyRotateCCW2: Int, keyRotateCW2: Int) =
  (
          'a'.toInt, 'd'.toInt, 'z'.toInt, 'x'.toInt, 'c'.toInt, 's'.toInt, 'w'.toInt, VK_ESCAPE,
          VK_LEFT, VK_RIGHT, VK_DOWN, VK_UP, '['.toInt, ']'.toInt)

  def pollState = {
    val state = State(_movement,_drop,_rotation,_option)
    pressedKeys.clear
    state
  }

  var pressedKeys: mutable.Set[Int] = mutable.Set.empty[Int]

  def isDown(keyCode: Int) = {
    pressedKeys.contains(keyCode)
  }

  def keyPressed(event: KeyEvent) = {
    pressedKeys += event.getKeyCode
  }

  private def _option = {
    if (isDown(keyQuit)) End
    else Option.None
  }

  private def _movement = {
    if (isDown(keyLeft) || isDown(keyLeft2)) Left
    else if (isDown(keyRight)|| isDown(keyRight2)) Right
    else Movement.None
  }
  private def _drop = {
    if(isDown(keyHardDrop)|| isDown(keyHardDrop2)) Hard
    else if(isDown(keySoftDrop)|| isDown(keySoftDrop2)) Soft
    else Drop.None
  }
  private def _rotation = {
    if (isDown(keyRotateCCW)|| isDown(keyRotateCCW2)) RotCCW
    else if (isDown(keyRotateCW)|| isDown(keyRotateCW2)) RotCW
    else if (isDown(keyRotate180)) Rot180
    else Rotation.None
  }

}