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

import _root_.scala.actors.Actor
import _root_.scala.actors.Actor._
import _root_.scala.xml.Elem
import mode.Playmode

/**
 * Manages state of a multiplayer battle.
 * @author will
 * @date Oct 14, 2008 8:12:28 PM
 */

case class AddStates(actor: GameActor, xml: Elem)
case class GetOpponentStates(actor: GameActor)

class Battle(val playerActor: GameActor, val opponentActor: GameActor, mode:Playmode.Value) extends BattleController with Actor {
  val seed = System.currentTimeMillis

  val playerController = new PlaybackController(seed,this)
  val opponentController = new PlaybackController(seed,this)
  var player = new Tetrion(playerController,this,mode)
  var opponent = new Tetrion(opponentController,this,mode)

  println("New Battle begun! player="+player+", opponent="+opponent)
  def act = loop { react {
    case AddStates(actor: GameActor, xml: Elem) =>
//      println("Battle: actor "+actor+" adding XML: "+xml)
      val tetrion = if (actor == playerActor) player else if(actor == opponentActor) opponent
              else {println("Error!"); null}
      val controller:PlaybackController = tetrion.Control.asInstanceOf[PlaybackController]
      XmlParse.addStates(controller, xml)
      // Cycle the Tetrion machine through all of the states just added
      do {
        tetrion.update
      } while(controller.peekAhead != EmptyState)
    case GetOpponentStates(actor: GameActor) =>
//      println("Battle: getting states for actor "+actor)
      val controller = if (actor == playerActor) opponentController else playerController
      if(player.get == player.GameOver) {
        reply(<Gameover youWin={(actor != playerActor).toString}/>)
      } else if(opponent.get == opponent.GameOver) {
        reply(<Gameover youWin={(actor != opponentActor).toString}/>)
      }
      reply(controller.toXmlIter)

    case BattleMessage(effect: BattleEffect, tetrion: Tetrion) =>
      // Ignore battle messages sent from the Tetrion - we only care about ones sent by the playback controllers
    println("Server-side tetrion battle message ignored.")

    case PlayBattleMessage(effect: (BattleEffect,Int), play: PlaybackController) =>
    println("Adding server-side playback battle message to history frame#"+effect._2)
      val tetrion = if(play == playerController) player else opponent
//      println("effect " + effect + " sent by playback controller to "+tetrion)
      tetrion ! PlayBattleMessage(effect, play)
    if(play.effects.contains(effect)) println("###ALREADY CONTAINS###!")
    else play.effects += effect
    case x => println("Unknown message: " + x)
  } }

  println("Starting battle!")
  start

  val players: List[Tetrion] = List(player,opponent)
}