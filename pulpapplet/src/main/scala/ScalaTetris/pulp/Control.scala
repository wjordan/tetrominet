package ScalaTetris.pulp

import ScalaTetris._
import pulpcore.Input._
import ScalaTetris.Rotation._
import ScalaTetris.Movement._
import ScalaTetris.Direction._
import ScalaTetris.Option._
import ScalaTetris.Drop._

/**
 * Input Controller for PulpCore.
 * @author will
 * @date Oct 5, 2008 10:22:58 AM
 */

object Player extends Enumeration("Player 1","Player 2") { val Player1, Player2 = Value }

class PulpControl(player : Player.Value, val seed: Long) extends Controller {
  def this(player: Player.Value) = this(player, System.currentTimeMillis)

  var (keyLeft: Int, keyRight: Int, keyRotateCCW: Int, keyRotateCW: Int, keyRotate180: Int,
       keySoftDrop: Int, keyHardDrop: Int, keyQuit:Int,
  keyLeft2:Int, keyRight2:Int, keySoftDrop2:Int, keyHardDrop2:Int,keyRotateCCW2:Int, keyRotateCW2: Int) =
  (if (player == Player.Player1)
    (KEY_A, KEY_D, KEY_Z, KEY_X, KEY_X, KEY_S, KEY_W, KEY_ESCAPE,
            KEY_LEFT,KEY_RIGHT,KEY_DOWN,KEY_UP,KEY_OPEN_BRACKET,KEY_CLOSE_BRACKET)
   else
    (KEY_NUMPAD4, KEY_NUMPAD6, KEY_NUMPAD7, KEY_NUMPAD9, KEY_NUMPAD2, KEY_NUMPAD5, KEY_NUMPAD8, KEY_F1,
            KEY_NUMPAD4, KEY_NUMPAD6,KEY_NUMPAD5, KEY_NUMPAD8,KEY_INSERT,KEY_HOME))

  def pollState = State(_movement,_drop,_rotation,_option)

  private def _option = {
    if (isDown(keyQuit)) End
//    else if (player == Player.Player1 && isPressed(KEY_1)) AddLine
    else Option.None
  }

  private def _movement = {
    if (isDown(keyLeft) || isDown(keyLeft2)) Left
    else if (isDown(keyRight)|| isDown(keyRight2)) Right
    else Movement.None
  }
  private def _drop = {
    if(isDown(keyHardDrop)|| isDown(keyHardDrop2)) Hard
    else if(isDown(keySoftDrop)|| isDown(keySoftDrop2)) Soft
    else Drop.None
  }
  private def _rotation = {
    if (isDown(keyRotateCCW)|| isDown(keyRotateCCW2)) RotCCW
    else if (isDown(keyRotateCW)|| isDown(keyRotateCW2)) RotCW
    else if (isDown(keyRotate180)) Rot180
    else Rotation.None
  }
}