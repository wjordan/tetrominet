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


case class SendController(control: Controller)

case class SendBattleEffect(effect: BattleEffect, control: Controller)

/**
 * Contains the Actors that communicate with the server-side components through HTTP.
 */
class Client {

  /** Schedules controller updates to fire at fixed intervals. */
  val TimerActor = new Actor { def act() = loop{
    UploadActor ! SendController(battle.input)
    Thread.sleep(1000)
  }}

  /** Fires player controller updates to the server and adds the reply sequence to the local opponent's playback. */
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

/**
 * A Battle is a live game session, consisting of a gameplay mode and one or more Tetrion.
 * In a multiplayer Battle, this is the interface where in-game effects and messages are passed
 * between players.
 */
trait BattleController {

  /** The list of active Tetrion that comprise this Battle instance. */
  var players: List[Tetrion]

  /** The current frame number the overall Battle has advanced to.
   * Note that in networked multiplayer Battles this time may only be synchronized to
   * a single reference Tetrion. */
  var time: Int = 0

  /** Passes BattleEffect messages back and forth between player and opponent.
   *  Note that this can be implemented either by an Actor or a normal method. */
  def !(msg:Any)

  def update: Unit = { time += 1; players.map(_.update) }
}

/**
 * Instantiate a Battle with one Tetrion controlled by local input and the other
 * controlled by a playback session.
 * Both player and opopnent Tetrion are initialized with the same Randomizer seed.
 */
class BattleLocal(seed: Long) extends BattleController {
  val latency = 1000

  // Hook the player Tetrion to the local PulpCore keyboard input.
  var input = new PulpControl(Player.Player1, seed)
  var player : Tetrion = new Tetrion(input,this)

  // Hook the opponent Tetrion to the playback controller receiving updates from the Client.
  var playback = new PlaybackController(seed,this)
  var opponent = new Tetrion(playback,this)

  var players = List(player, opponent)

  override def update = {
    time += 1
    player.update
    if(time > latency*2) {
      opponent.update
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
    // INFO: Using Actors for processing battle effects only and not for the main game loop for performance.
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