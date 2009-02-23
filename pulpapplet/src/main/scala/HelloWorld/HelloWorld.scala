/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

package HelloWorld


import java.net.URL

import pulpcore.net.Upload
import pulpcore.scala.PulpCore._
import pulpcore.animation.Easing._
import pulpcore.image.Colors._
import pulpcore.scene.Scene2D
import pulpcore.sprite._
import Tetromi._
import mode.Playmode

import Tetromi.local.SinglePlayerGame
import Tetromi.net
import Tetromi.net._

import Tetromi.pulp._
import pulpcore.Input
import pulpcore.Stage
import scala.io.Source
import scala.xml._

class HelloWorld extends Scene2D {
  lazy val normalButton = Button.createLabeledButton("Normal", 50, 200)
  lazy val deathButton = Button.createLabeledButton("Death", 150, 200)
  lazy val player1 = new PulpControl(Player.Player1)
  var client: Client = null

  override def load = {
    Stage.setFrameRate(20);
    add(new FilledSprite(WHITE))
    add(new Label("Waiting for opponent...",50,100))
    add(new Label("WASD keys to move, [] keys to rotate",50,130))
    add(new Label("Or arrow keys to move, ZX keys to rotate",50,160))
    add(normalButton)
    add(deathButton)

    client = new Client(new ClientView{
      def gameOver(iWin:Boolean): Unit = Stage.setScene(new GameOverScene(iWin))
      def gameStart(seed: Long): Unit = {
        println("Loading tetromi scene!")
        Stage.setScene(new TetromiScene(new BattleLocal(new PulpControl(Player.Player1,seed)),HelloWorld.this.client))
      }
    })

  }

  override def update(elapsedTime:Int) = {
    super.update(elapsedTime)
    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
    if(normalButton.isClicked) {
      Stage.replaceScene(new SoloTetromiScene(new SinglePlayerGame(player1,Playmode.Easy)))
    }
    if(deathButton.isClicked) {
      Stage.replaceScene(new SoloTetromiScene(new SinglePlayerGame(player1,Playmode.Death)))
    }
  }

}

class SoloTetromiScene(battle: BattleController) extends Scene2D {
  var pulpview1: PulpTetrionView = null

  override def load = {
    add(new FilledSprite(WHITE))
    pulpview1 = new PulpTetrionView(battle.players(0), 20)
    pulpview1.setLocation(50,20)
    add(pulpview1)
  }

  var paused = false

  override def update(elapsedTime:Int) = {
    super.update(elapsedTime)
    if(Input.isPressed(Input.KEY_P)) paused = !paused
    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
    battle.update
  }
}

class TetromiScene(battle: BattleLocal, client: Client) extends Scene2D {
  var pulpview1: PulpTetrionView = null
  var pulpview2: PulpTetrionView = null


  override def load = {
    add(new FilledSprite(WHITE))

    pulpview1 = new PulpTetrionView(battle.player, 20)
    pulpview1.setLocation(50,20)
    add(pulpview1)

    pulpview2 = new PulpTetrionView(battle.opponent, 20)
    pulpview2.setLocation(300,20)
    add(pulpview2)

    client.startGame(battle)
  }

  var paused = false

  override def update(elapsedTime:Int) = {
    super.update(elapsedTime)
    if(Input.isPressed(Input.KEY_P)) paused = !paused
    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
    battle.update
  }

}

class GameOverScene(won: Boolean) extends Scene2D {
  val gameoverlabel: Label = new Label(if(won)"You win!" else "You lose!",50,100)

  override def load = {

    add(new FilledSprite(20,50,300,200,WHITE))
    add(gameoverlabel)
    add(new Label("Press R to play again!",50,200))
  }

  override def update(elapsedTime:Int) = {
    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
  }
}