package ScalaTetris.model


import scala.collection.mutable.{SynchronizedMap, HashMap}
import scala.collection.mutable
import ScalaTetris.net.{BattleEffect, AddLinesEffect}
import ScalaTetris._

/** Defines a single state. */
trait GState {
  /** Executed once per frame. */
  def run: Unit = {}
  /** Called when the state is first entered
   * @return the number of frames before the state expires */
  def enter: Int = -1
  /** Called when the state's counter expires.
   * @return the new State to enter. */
  def exit: GState = this
}

/**
 * The finite state machine implementing the core Tetrion game logic.
 * @author will
 * @date Oct 23, 2008 4:23:52 AM
 */
trait TetrionState {
  this: Tetrion =>
  var state: GState = this.Spawn
  var control: State = EmptyState

  var lineAdd = 0
  var frame = 0

  val battleEffects = mutable.Set.empty[BattleEffect]

  /** Assertion-Debugging variables */
/*
  val debug = false
  val stateMap = new HashMap[Int,GState] with SynchronizedMap[Int,GState]
  val piecePosMap = new HashMap[Int,Pos] with SynchronizedMap[Int,Pos]
  val controlMap = new HashMap[Int,State] with SynchronizedMap[Int,State]
*/

  def get: GState = state

  /** Enters a new game state, updating the timer counter.
   * @return the updated timer counter (so state redirects can be chained) */
  def set(s: GState): Int = {
    state = s;
    counter = state.enter;
    notifyView(_.updateState)
    counter
  }

  def run(c: State): Unit = {
    control = c
    if (state == GameOver) return
    if (control.option == Option.Empty) { return} //println("empty!");

/*
    if(debug) {
      // Simple synchronization unit tests
      stateMap + (frame -> state)
      if(this == battleController.opponent) assert(stateMap(frame) == battleController.player.stateMap(frame), "statemap "+this+": "+stateMap(frame)+" !="+battleController.player.stateMap(frame))
      piecePosMap + (frame -> piecePos)
      if(this == battleController.opponent) assert(piecePosMap(frame) == battleController.player.piecePosMap(frame), "piecePosMap "+this+": "+piecePosMap(frame)+" !="+battleController.player.piecePosMap(frame))
    }
*/

/*
    if(debug) {
      controlMap + (frame -> control)
      if(this == battleController.opponent) assert(controlMap(frame) == battleController.player.controlMap(frame), this+": "+controlMap(frame)+" !="+battleController.player.controlMap(frame))
    }
*/

    if (control.option == Option.End) set(GameOver)
    if (control.option == Option.AddLine) lineAdd += 1
    battleEffects.foreach(_.run)
    incDAS(control.movement)
    counter -= 1
    if (counter == 0) set(state.exit)
    else state.run

    // Increase total frame counter at the end of the frame
    frame += 1
  }

  // This simple logic causes rotations to occur only once per unique button input.
  var rotateVal: Rotation.Value = Rotation.None
  def rotated(rot: Rotation.Value): Boolean =
    if(rotateVal == rot) false else { rotateVal = rot; true }

  object Falling extends GState {
    override def enter = {rotateVal = control.rotation; -1}
    override def run = {
      if(rotated(control.rotation)) rotate(rotateVal) // 1. Rotation / Wall Kicks
      if (isBlockOut) set(GameOver) // 2. Check for "block out"
      doMovement(control.movement) // 3. Lateral Movement
      doGravity(control.drop) // 4. Gravity / Line clears
    }
  }

  object Paused extends GState

  /** Are is the delay in between piece lock and next piece spawn */
  object Are extends GState {
    // Activate any battle effects in the queue
    override def enter = {
      if (!battleQueue.isEmpty) {
        val effect = battleQueue.dequeue
        battleEffects += effect
        effect.set(TetrionState.this)
      }
      else if (lineAdd > 0) set(LineAdd)
      else are
    }
    override def exit = Spawn
  }

  object GameOver extends GState {
    override def enter = { println("Game OVER!"); -1 }
  }

  object Spawn extends GState {
    override def enter =
      if (!setNewPiece(control.rotation)) set(GameOver)
      else 1
    override def exit = Falling
  }

  /** Extra delay is added whenever line(s) are cleared */
  object LineClear extends GState {
    override def enter =
      if (checkLineClears) {
        if (clearedBlocks.length >= 2) battleController ! (BattleMessage(new AddLinesEffect(clearedBlocks), TetrionState.this))
        lineClearDelay
      } else set(Are)
    override def exit = {
      for (lineClear <- clearedLines) lineClear.map(_.shiftDown)
      notifyView(_.lineClearEnd)
      Are
    }
  }

  object LineAdd extends GState {
    override def enter = 10
    override def exit = {
      for (i <- 0 until lineAdd) bottomRow.map(_.shiftUp(InvalidCell.block))
      notifyView(_.lineClearEnd)
      lineAdd = 0
      Are
    }
  }

  
  var counter: Int = state.enter
}