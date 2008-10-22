package HelloWorld

import pulpcore.scala.PulpCore._
import pulpcore.animation.Easing._
import pulpcore.image.Colors._
import pulpcore.scene.Scene2D
import pulpcore.sprite._
import ScalaTetris._
import ScalaTetris.net
import ScalaTetris.net._
import ScalaTetris.pulp._
import pulpcore.Input
import pulpcore.Stage
import scala.io.Source
import scala.xml._
import ScalaTetris.server.HighScoreChecker

class HelloWorld extends Scene2D {
  var client: Client = null

  override def load = {
    Stage.setFrameRate(20);
    add(new FilledSprite(WHITE))
    add(new Label("Waiting for opponent...",50,100))

    client = new Client
  }

  override def update(elapsedTime:Int) = {
    if(client.started == true) {
      println("Loading tetris scene!")
      Stage.setScene(new TetrisScene(client.battle))
    }

    if(Input.isPressed(Input.KEY_R)) {
      Stage.replaceScene(new HelloWorld)
    }
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

    battle.update(elapsedTime)
  }

}

class GameOverScene(gameOverText: String) extends Scene2D {
  val gameoverlabel: Label = new Label(gameOverText,50,100)

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