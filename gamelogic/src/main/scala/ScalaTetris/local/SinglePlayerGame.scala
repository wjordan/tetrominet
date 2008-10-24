package ScalaTetris.local

import net.BattleController
import pulp.{PulpControl, Player}

/**
 * Creates a single player game run without any communication over the network.
 * (Any High score entries will still be replayed and verified upon submission.)
 * @author will
 * @date Oct 22, 2008 11:49:46 PM
 */

class SinglePlayerGame(seed:Long) extends BattleController {

  // Hook the player Tetrion to the local PulpCore keyboard input.
  var input = new PulpControl(Player.Player1, seed)
  var player: Tetrion = new Tetrion(input,this)

  var players: List[Tetrion] = List(player)

  def !(msg: Any) = msg match {
      case x => println("Ignoring battle effect in single player game: "+x)
  }
}