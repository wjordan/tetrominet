/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

package Tetromi.local


import mode.Playmode
import net.BattleController

/**
 * Creates a single player game run without any communication over the network.
 * (Any High score entries will still be replayed and verified upon submission.)
 * @author will
 * @date Oct 22, 2008 11:49:46 PM
 */

class SinglePlayerGame(input: Controller,mode:Playmode.Value) extends BattleController {

  // Hook the player Tetrion to the local PulpCore keyboard input.
  val player: Tetrion = new Tetrion(input,this,mode)

  val players: List[Tetrion] = List(player)

  def !(msg: Any) = msg match {
      case x =>
  }
}