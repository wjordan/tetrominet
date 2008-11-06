package ScalaTetris.net


import mode.PlayMode
import mode.Playmode


import scala.collection.{mutable, Map}
import java.io.{ByteArrayInputStream, IOException}
import java.util.Collections.UnmodifiableRandomAccessList
import scala.xml.Elem
import ScalaTetris._
import scala.actors.Actor
import scala.actors.Actor._
import java.net._;


case class SendController(control: Controller)
case class SendBattleEffect(effect: BattleEffect, control: Controller)

/** Abstract data upload interface. */
abstract class AbstractUpload(url: URL) {
  def addField(field:String, value:String): Unit
  def sendNow: Unit
  def getResponse: String
  def getCookie: Option[String]
}

trait ClientView {
  def gameOver(iWin:Boolean): Unit
  def gameStart(seed: Long): Unit
  def upload(url:URL) = new PulpUpload(url)
}

object Client {
  val server2 = "tetromi.net"
  val server = "localhost:8080"
}

/**
 * Contains the Actors that communicate with the server-side components through HTTP.
 */
class Client(view: ClientView) {
  
  def startGame(b: BattleLocal) {
    battle = b
    TimerActor.start
  }

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
//        println("Sending state")
        val u: AbstractUpload = view.upload(new URL("http://"+Client.server+"/api/addStates" + sessionId))
        u.addField("seed", control.seed.toString)
        u.addField("xml", (control.toXmlIter).toString)
        u.sendNow
        // Add the received controller inputs to the opponent's playback controller
        val xmlElem = scala.xml.XML.loadString(u.getResponse)
//        println("Received response, xml = "+xmlElem)
        xmlElem.label match {
          case "Controller" => XmlParse.addStates(battle.playback,xmlElem)
          case "Gameover" =>
            val iWin = (xmlElem \\ "@youWin").text
            view.gameOver(iWin == "true")
            self.exit
        }

      }
  }}}

  var battle: BattleLocal = null

  var sessionId = ""

  /** Perform long polling on the server until a game connection is established. */
  val WaitForStartActor = actor { loop{
//    println("Waiting for other player(s)...")
    val url = new URL("http://"+Client.server+"/api/waitForStart" + sessionId)
    val u: AbstractUpload = view.upload(url)
    try {
      u.sendNow

      // Manually parse and store the session ID if a cookie is set in the response header
      u.getCookie match {
        case Some(s) => {
          val jsession = s.substring(s.indexOf("="),s.indexOf(";"))
          sessionId = ";jsessionid"+jsession
        }
        case None =>
      }

      val xmlElem = scala.xml.XML.loadString(u.getResponse)
      xmlElem.label match {
        case "TIMEOUT" =>
//          println("Time out, continue wait cycle...")
        case "GAMESTART" =>
          val seed = (xmlElem \ "@seed").text.toLong
//          println("Game start! Seed=" + seed)
          view.gameStart(seed)
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
  val players: List[Tetrion]

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
class BattleLocal(val input: Controller) extends BattleController {
  val latency = 10
  val playMode: Playmode.Value = Playmode.Easy
  // Hook the player Tetrion to the local PulpCore keyboard input.
  val player : Tetrion = new Tetrion(input,this,playMode)

  // Hook the opponent Tetrion to the playback controller receiving updates from the Client.
  val playback = new PlaybackController(input.seed,this)
  val opponent = new Tetrion(playback,this,playMode)

  val players = List(player, opponent)

  /** Passes BattleEffect messages from opponent to the local player. */
  def !(msg: Any) = msg match {
    // INFO: Using Actors for processing battle effects only and not for the main game loop for performance.
      case BattleMessage(effect: BattleEffect, tetrion: Tetrion) =>
//        println("effect " + effect + " sent by " + tetrion + "!")
        if (tetrion == opponent) player ! BattleMessage(effect, tetrion)
//        else println("ignoring effect trigger sent from player to opponent")
      case PlayBattleMessage(effect: (BattleEffect, Int), play: Controller) =>
//        println("effect " + effect + " sent by playback controller to "+opponent)
        opponent ! PlayBattleMessage(effect, play)
      case x => println("Unknown message: "+x)
  }

}