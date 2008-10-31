package ScalaTetris.local

import net.BattleController

/**
 * Creates a single player game run without any communication over the network.
 * (Any High score entries will still be replayed and verified upon submission.)
 * @author will
 * @date Oct 22, 2008 11:49:46 PM
 */

class SinglePlayerGame(input: Controller) extends BattleController {

  // Hook the player Tetrion to the local PulpCore keyboard input.
  val player: Tetrion = new Tetrion(input,this)

  val players: List[Tetrion] = List(player)

  def !(msg: Any) = msg match {
      case x =>
  }
}