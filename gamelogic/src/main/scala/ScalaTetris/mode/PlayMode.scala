package ScalaTetris.mode

/**
 * Different gameplay modes (difficulty curves, scoring, win/loss conditions, etc)
 * @author will
 * @date Oct 24, 2008 7:15:39 PM
 */
abstract class PlayMode {
  /**Gravity expressed in number of rows descended per frame (TGM works in fractions of 256 so we'll follow that convention) */
  var gravity: Double

  /**Lock Delay is the max amount of frames a piece rests on ground level before locking */
  var maxLockDelay: Int

  /**Are is the normal 'eh?' pause in between a piece lock and the next piece spawn */
  var are: Int

  /**Line Clear Delay is length of the pause after lines are cleared */
  var lineClearDelay: Int

  /**Whatever 'Score' means in this mode */
  var score: Int

  /**Logic to perform each frame */
  def update: Unit = {}

  /**Logic to perform after a line clear */
  def lineClear(numLines: Int): Unit = {}

  /**Logic to perform after a piece is locked */
  def pieceLock: Unit = {}
}

object Playmode extends Enumeration("Easy", "Death") {
  val Easy, Death = Value

  def apply(p: Playmode.Value, tetrion: Tetrion): PlayMode = p match {
    case Easy => new EasyMode(tetrion)
    case Death => new TADeath(tetrion)
  }
}

/**TGM-style level advancement. */
trait TgmLevelAdvance extends PlayMode {
  /**Segment difficulty by 100-level chunks */
  val seg = 100
  var level = 0

  override def lineClear(numLines: Int) = {
    level += numLines
    if (level % seg < numLines) {
      level -= level % seg
    }
  }

  override def pieceLock = {if (level % seg < seg - 1) level += 1}
}

/**Standard easymode. Start off ridiculously slow, then ramp up difficulty curve based on
 * number of lines cleared. */
class EasyMode(tetrion: Tetrion) extends PlayMode with TgmLevelAdvance {
  var maxLockDelay = 30
  var are = 25
  var lineClearDelay = 40
  var gravity = (4.0 / 256)

  var score = 0

  /**Gravity curve as specified on TetrisConcept wiki */
  val gravCurve = Array((0, 4), (8, 5), (19, 6), (35, 8), (40, 10), (50, 12), (60, 16), (70, 32), (80, 48),
    (90, 64), (100, 4), (108, 5), (119, 6), (125, 8), (131, 12), (139, 32), (149, 48), (156, 80), (164, 112),
    (174, 128), (180, 144), (200, 16), (212, 48), (221, 80), (232, 112), (244, 144), (256, 176), (267, 192),
    (277, 208), (287, 224), (295, 240), (300, 5120))
  var curGrav = 0

  private def grav(lvl: Int): Double = {
    if (curGrav + 1 < gravCurve.length && gravCurve(curGrav + 1)._1 <= level) curGrav += 1
    (gravCurve(curGrav)._2 / 256.0)
  }

  override def update = {
    score = level
    gravity = grav(level)
  }

  override def lineClear(numLines: Int) = {
    super.lineClear(numLines)
    if (level == 300) println("Congratulations! You win!")
  }
}

/**"TA Death" mode. Starts immediately at 20G and adjusts the timings as
 *  levels advance. */
class TADeath(tetrion: Tetrion) extends PlayMode with TgmLevelAdvance {
  /* Curve as specified on TetrisConcept wiki */
  val curvePoints = Array(0, 100, 200, 300, 400, 500)
  val areCurve = Array(18, 14, 14, 8, 7, 6)
  val areLineCurve = Array(12, 6, 6, 6, 5, 4)
  val dasCurve = Array(12, 12, 11, 10, 8, 8)
  val lockCurve = Array(30, 26, 22, 18, 15, 15)
  val lineClearCurve = Array(12, 6, 6, 6, 5, 4)

  var curve = 0

  var score = 0
  var are = areCurve(0)
  var lineClearDelay = lineClearCurve(0)
  var maxLockDelay = lockCurve(0)
  var gravity = 20.0

  override def lineClear(numLines: Int) = {
    super.lineClear(numLines)
    if (curve + 1 < curvePoints.length && curvePoints(curve + 1) <= level) {
      curve += 1
      are = areCurve(curve)
      lineClearDelay = lineClearCurve(curve)
      maxLockDelay = lockCurve(curve)
      println("Upgrading level!!")
      println("new are=" + are)
    }
  }

  override def update = {
    score = level
  }

}