package ScalaTetris.net


import _root_.pulpcore.net.{Upload, Download}
import HelloWorld.GameOverScene
import java.io.{ByteArrayInputStream, IOException}
import java.util.Collections.UnmodifiableRandomAccessList
import scala.xml.Elem
import ScalaTetris._
import pulp.{PulpControl, Player}
import pulpcore.CoreSystem
import pulpcore.Stage
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable
import java.net._;


/**
 * Multi-player Client that communicates with a server through an HTTPConnection.
*/

case class SendController(control: Controller)

case class SendBattleEffect(effect: BattleEffect, control: Controller)

object Client {
  val server: String = "http://ec2-75-101-224-153.compute-1.amazonaws.com:8080"
}

class Client {

  /** Schedules controller updates to fire at fixed intervals. */
  val TimerActor = new Actor { def act() = loop{
    UploadActor ! SendController(battle.input)
    Thread.sleep(1000)
  }}

  /** Fires player controller updates to the server and adds the reply sequence to the local opponent playback. */
  val UploadActor = actor{loop{react{
    case SendController(control) =>
      if(control.length - control.lastFrameDump > 0) {
        // Send the player's recorded controller to the server
        println("Sending state")
        val u: Upload = new Upload(new URL(CoreSystem.getBaseURL,"/api/addStates" + sessionId))
        u.addField("seed", control.seed.toString)
        u.addField("xml", (control.toXmlIter).toString)
        u.sendNow
        // Add the received controller inputs to the opponent's playback controller
        val xmlElem = scala.xml.XML.loadString(u.getResponse)
        println("Received response, xml = "+xmlElem)
        xmlElem.label match {
          case "Controller" => XmlParse.addStates(battle.playback,xmlElem)
          case "Gameover" =>
            val iWin = (xmlElem \\ "@youWin").text
            Stage.setScene(new GameOverScene("Game over! " + (if(iWin == "true")"You win!" else "You lose!")))
            self.exit
        }

      }
  }}}

  var started = false
  var battle: BattleLocal = null

  var sessionId = ""

  /** Perform long polling on the server until a game connection is established. */
  val WaitForStartActor = actor { loop{
    println("Waiting for other player(s)...")
    val url = new URL(CoreSystem.getBaseURL, "/api/waitForStart" + sessionId)
    println("url="+url)
    val u: Upload = new Upload(url)
    try {
      u.sendNow
      // Maunally parse and store the session ID if a cookie is set in the response header
      val c = u.getResponseFields.get("Set-Cookie").asInstanceOf[java.util.List[String]]
      if (c != null && c.size > 0) {
        val s = c.get(0)
        val jsession = s.substring(s.indexOf("="),s.indexOf(";"))
        println("Setting " + jsession + "!")
        sessionId = ";jsessionid"+jsession
      }
      val xmlElem = scala.xml.XML.loadString(u.getResponse)
      xmlElem.label match {
        case "TIMEOUT" =>
          println("Time out, continue wait cycle...")
        case "GAMESTART" =>
          val seed = (xmlElem \ "@seed").text.toLong
          println("Game start! Seed=" + seed)
          battle = new BattleLocal(seed)
          started = true
          TimerActor.start
          self.exit
        case _ => println("Error"); self.exit
      }
    } catch {
      case e: IOException =>
        println("Error in wait, exiting!")
        self.exit
    }
  }}

}

/** The Tetrion uses this interface to communicate messages that affect a multiplayer Battle. */
trait BattleController {
  var player: Tetrion
  var opponent: Tetrion
  var time: Int

  /** Passes BattleEffect messages back and forth between player and opponent. */
  def !(msg:Any)
}

/** Instantiate a local battle. Both player and opopnent Tetrion use the same Randomizer seed. */
class BattleLocal(seed: Long) extends BattleController {
  val latency = 1000
  var time = 0

  // Hook the player Tetrion to the local PulpCore keyboard input.
  var input = new PulpControl(Player.Player1, seed)
  var player : Tetrion = new Tetrion(input,this)

  // Hook the opponent Tetrion to the playback controller that will receive updates from the Client.
  var playback = new PlaybackController(seed,this)
  var opponent = new Tetrion(playback,this)

  def update(elapsedTime: Int) = {
    time += elapsedTime
    player.update(elapsedTime)
    if(time > latency*2) {
//      XmlParse.addStates(playback, record.toXmlIter)
      opponent.update(elapsedTime)
    }
  }

  def restart = {
    input = new PulpControl(Player.Player1)
    player = new Tetrion(input,this)

    playback = new PlaybackController(input.seed,this)
    opponent = new Tetrion(playback,this)
  }

  /** Passes BattleEffect messages from opponent to the local player. */
  def !(msg: Any) = msg match {
    // INFO: Using Actors for processing battle effects only and not for the main game loop due to performance hits.
      case BattleMessage(effect: BattleEffect, tetrion: Tetrion) =>
        println("effect " + effect + " sent by " + tetrion + "!")
        if (tetrion == opponent) player ! BattleMessage(effect, tetrion)
        else println("ignoring effect trigger sent from player to opponent")
      case PlayBattleMessage(effect: (BattleEffect, Int), play: Controller) =>
        println("effect " + effect + " sent by playback controller to "+opponent)
        opponent ! PlayBattleMessage(effect, play)
      case x => println("Unknown message: "+x)
  }

}