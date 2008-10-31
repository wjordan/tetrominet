package HelloWorld


import java.net.URL

import pulpcore.net.Upload
import pulpcore.scala.PulpCore._
import pulpcore.animation.Easing._
import pulpcore.image.Colors._
import pulpcore.scene.Scene2D
import pulpcore.sprite._
import ScalaTetris._

import ScalaTetris.local.SinglePlayerGame
import ScalaTetris.net
import ScalaTetris.net._

import ScalaTetris.pulp._
import pulpcore.Input
import pulpcore.Stage
import scala.io.Source
import scala.xml._

class HelloWorld extends Scene2D {
  var client: Client = null
  lazy val normalButton = Button.createLabeledButton("Normal", 50, 200)
  lazy val deathButton = Button.createLabeledButton("Death", 150, 200)
  lazy val player1 = new PulpControl(Player.Player1)
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
        println("Loading tetris scene!")
        Stage.setScene(new TetrisScene(new BattleLocal(new PulpControl(Player.Player1,seed))))
      }

      def upload(url:URL) = { new PulpUpload(url)}
    })
  }

  override def update(elapsedTime:Int) = {
    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
    if(normalButton.isClicked) {
      Stage.replaceScene(new SoloTetrisScene(new SinglePlayerGame(player1)))
    }
    if(deathButton.isClicked) {
      Stage.replaceScene(new SoloTetrisScene(new SinglePlayerGame(player1)))
    }
  }

}

class SoloTetrisScene(battle: BattleController) extends Scene2D {
  var pulpview1: PulpTetrionView = null

  override def load = {
    add(new FilledSprite(WHITE))
    pulpview1 = new PulpTetrionView(battle.players(0), 20)
    pulpview1.setLocation(50,20)
    add(pulpview1)
  }

  var paused = false

  override def update(elapsedTime:Int) = {
    if(Input.isPressed(Input.KEY_P)) paused = !paused
    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
    battle.update
  }
}

class TetrisScene(battle: BattleLocal) extends Scene2D {
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

  }

  var paused = false

  override def update(elapsedTime:Int) = {
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