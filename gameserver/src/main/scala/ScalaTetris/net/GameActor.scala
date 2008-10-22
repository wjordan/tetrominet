package ScalaTetris.net

import _root_.scala.actors._
import _root_.scala.actors.Actor._
import _root_.scala.collection.mutable.{Set, HashMap}
import bootstrap.liftweb.StartMessage

/**
 * @author will
 * @date Oct 17, 2008 1:54:58 AM
 */

case class StartGameMessage(battle: Battle)
object WaitMessage

/**
 * This is the main session variable for each individual user.
 * Its main purpose is to perform message handling with the game waiting list.
 */
class GameActor extends Actor {
  var name = NameList.getNextName
  var currentGame = "game"
  var game: Battle = null

  /** Wait 5 seconds for a start message then timeout */
  def act = loop {
    react{
      case WaitMessage =>
        val waiting: Set[GameActor] = GameList(this)
        println("Game " + currentGame + " has " + waiting.size + " waiting participants")
        if (waiting.size > 0) {
          println("Someone else is waiting too!")
          // Create the new game object with one other player TODO: Generalize to X players
          val battle: Battle = new Battle(this, waiting.toList(0))
          GameList.gameList += (currentGame -> battle)
          game = battle
          // Send a StartMessage to the other waiting GameActor(s)
          waiting.map(x => x ! StartGameMessage(battle))
          // Reply with a StartMessage to this GameActor

          reply(StartMessage(battle.seed))
        } else {
          GameList.add(this)
          // Long polling wait
          println("long polling wait, gamelist="+GameList(this).size)
          val x = receiveWithin(5000){
            case StartGameMessage(battle: Battle) =>
              println("Received Game start message!!")
              game = battle
              StartMessage(battle.seed)
            case TIMEOUT => TIMEOUT
          }
          GameList.remove(this)
          reply(x)
        }

    }
  }

  start

  override def toString = "User:"+name
}

object NameList {
  // List of active user names
  var nameList = List[String]()

  def getNextName = {
    val name = "Guest#"+nameList.length
    nameList += name
    name
  }
}

object GameList {
  def add(user : GameActor) = {
    val set = waitList(user.currentGame) + user
    waitList(user.currentGame) = set
  }

  def length = waitList.size

  def remove(user : GameActor) = {
    val set = waitList(user.currentGame) - user
    waitList(user.currentGame) = set
  }

  import scala.collection.mutable
  /** Maps game names to a list of GameActors */
  val waitList = new HashMap[String, mutable.Set[GameActor]] {
    // Create an empty Set for new game requests
    override def default(key: String) = mutable.Set.empty[GameActor]
  }
  def apply(s: String): Set[GameActor] = waitList(s)
  def apply(user: GameActor): Set[GameActor] = waitList(user.currentGame)

  /** Maps game names to active game Battles */
  val gameList = new HashMap[String, Battle]
}

