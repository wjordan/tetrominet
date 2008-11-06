package ScalaTetris.local


import mode.Playmode
import net.BattleController

/**
 * Creates a single player game run without any communication over the network.
 * (Any High score entries will still be replayed and verified upon submission.)
 * @author will
 * @date Oct 22, 2008 11:49:46 PM
 */

class SinglePlayerGame(input: Controller,mode:Playmode.Value) extends BattleController {

  // Hook the player Tetrion to the local PulpCore keyboard input.
  val player: Tetrion = new Tetrion(input,this,mode)

  val players: List[Tetrion] = List(player)

  def !(msg: Any) = msg match {
      case x =>
  }
}