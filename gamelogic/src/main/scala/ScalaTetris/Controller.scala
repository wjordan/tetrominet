package ScalaTetris
import net.{BattleEffect, BattleController}
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.{SynchronizedBuffer, ArrayBuffer, Buffer}
import scala.xml._
import ScalaTetris.net._

/**
 * Interface for managing the input states from the GUI that provides recording and playback functionality.
 * @author will
 * @date Oct 8, 2008 7:23:27 PM
 */

object Option extends Enumeration("End","None","AddLine") { val End, None, AddLine, Empty = Value }

/** A set of the different input state enumerations, with a binary encoding. */
case class State (movement: Movement.Value, drop: Drop.Value, rotation: Rotation.Value, IRS: Rotation.Value, option: Option.Value) {
  def this(int: Int) = this(
    Movement.apply(int & 0x3),
    Drop.apply((int >> 2) & 0x3),
    Rotation.apply((int >> 4) & 0x3),
    Rotation.apply((int >> 6) & 0x3),
    Option.apply((int >> 8) & 0x7))
  def toInt: Int = (movement.id) + (drop.id << 2) + (rotation.id << 4) + (IRS.id << 6) + (option.id << 8)
}

object EndState extends State(Movement.None,Drop.None,Rotation.None,Rotation.None,Option.End)
object EmptyState extends State(Movement.None,Drop.None,Rotation.None,Rotation.None,Option.Empty)
object NoneState extends State(Movement.None,Drop.None,Rotation.None,Rotation.None,Option.None)

object EmptyController extends Controller{ val seed = 0L; def pollState = EmptyState }

/** Encapsulates a run-length encoded sequence of States. */
case class StateSeq(state : State, length : Int) {
  def this(state : State) = this(state,1)
  var len = length
  def toXML = <State id={state.toInt.toString} len={len.toString}/>
  def toSeq = (for(x <- 0 until len) yield state)
  override def toString="State="+state+",len="+len
}

object StateSeq {
  /** Packs a sequence of States into a StateSeq array using simple run-length encoding. */
  implicit def toStateSeq(seq : Seq[State]) : Seq[StateSeq] = {
    if(seq.length == 0) return Seq.empty

    val stateSeq = new ArrayBuffer[StateSeq]()
    var state: StateSeq = new StateSeq(seq(0),0)
    for(i <- seq) {
      if(state.state == i) state.len += 1
      else { stateSeq += state; state = new StateSeq(i) }
    }
    stateSeq += state
    stateSeq
  }
}

object Controller extends Enumeration{ val history, poll, xml = Value }
trait Controller {
  import StateSeq._
  
/*
  def act() = loop { react {
//    case Controller.history => reply(history)
//    case Controller.poll => reply(poll)
//    case Controller.xml => reply(toXmlIter)
  }}
  start
*/

  /** Holds the fixed random-number seed to be used in the game's execution. */
  val seed : Long
  /** Returns the current set of input states. */
  protected def pollState: State

  /** These buffers need to be synchronized because they are modified from the Tetrion */
  private val _history = new ArrayBuffer[State]() with SynchronizedBuffer[State]
  val effects = new ArrayBuffer[(BattleEffect,Int)]() with SynchronizedBuffer[(BattleEffect,Int)]

  protected def history: Seq[StateSeq] = _history
  protected def history(from: Int): Seq[StateSeq] = _history.drop(from)

  /** Adds the current state to the sequence history. */
  private final def addState(pollState : State) = synchronized {
    _history += pollState;
    pollState
  }

  /** Polls and latches the controller input */
  final def poll = addState(pollState)
  
  var lastFrameDump = 0
  def length = _history.length

  /** Outputs only the controller history accumulated since the last call to this function. */
  def toXmlIter = synchronized {
    val out = toXML2(lastFrameDump)
    lastFrameDump = length
    out
  }
  
  /** Only takes the sequence of effects that occurred on or after the specified frame. */
  def effects(from: Int): Seq[(BattleEffect,Int)] = synchronized { effects.dropWhile(_._2 < from) }

  def += (effect: BattleEffect) = synchronized {
    println("Adding effect "+effect+" to history#" + (length))
    effects += (effect,length)
  }

  /** Outputs the entire controller history to an XML string. */
  def toXML = <Controller seed={seed.toString}>{history.map(_.toXML).toList}</Controller>

  /** Outputs the recent controller history to an XML string. */
  def toXML2(from: Int) = synchronized {
    val seqLength = _history.drop(from).length 
    <Controller seed={seed.toString} startFrame={from.toString} endFrame={length.toString} length={seqLength.toString}>{history(from).map(_.toXML).toList}
    {for(effect <- effects(from)) yield{<BattleEffect frame={effect._2.toString}>{effect._1.toXml}</BattleEffect>}.toList}
    </Controller>
  }
}

object XmlParse {

  private def stateFromXML(node: Node): StateSeq = new StateSeq(new State(
    (node \ "@id").text.toInt),
    (node \ "@len").text.toInt)

  private def statesFromXML(states: Seq[Node]) = {
    for(state @ <State>{_*}</State> <- states) yield XmlParse.stateFromXML(state)
  }

  def battleFromXML(states: Seq[Node]) = {
    for(effect @ <BattleEffect>{_*}</BattleEffect> <- states) yield {(BattleEffect.fromXml(effect), (effect \ "@frame").text.toInt)}
  }

  def addStates(c: PlaybackController, elem: Elem) = elem match {
    case <Controller>{states @ _*}</Controller> => {
  println("Controller:adding states")
      c ++= statesFromXML(states)
      battleFromXML(states).map(c addBattle _)
    }
    case _ =>
  }

/*
  def PlaybackFromXML(elem: Elem): PlaybackController = elem match {
    case <Controller>{states @ _*}</Controller> =>
      new PlaybackController(statesFromXML(states), (elem \ "@seed").text.toInt)
    case _ => null
  }
*/
}

/** Simple controller to playback a recorded session. */
class PlaybackController(val seed: Long, battleController: BattleController) extends Controller {
  def this(array: Seq[StateSeq], s: Long, battle: BattleController) = { this(s,battle); this ++= array }

  // Note: These arrays must be Synchronized because of updates coming from the UploadActor.
  val hist = new ArrayBuffer[State]() with SynchronizedBuffer[State]
  val battleHist = new ArrayBuffer[(BattleEffect,Int)]() with SynchronizedBuffer[(BattleEffect,Int)]

  var currentFrame = -1

  /** Peeks at the most recent controller state, but doesn't actually advance the sequence. */
  def peek = synchronized { if(currentFrame >= hist.length || currentFrame < 0) EmptyState else hist(currentFrame) }
  /** Looks ahead at the next state that will be returned by PollState. */
  def peekAhead = synchronized { if(currentFrame+1 >= hist.length) EmptyState else hist(currentFrame+1)  }
  
  def pollState: State = synchronized {
    currentFrame += 1;
    val state = peek
    if(state == EmptyState) currentFrame -= 1 // No side effects
    else {
      sendBattleEffects;
    }
    state
  }
  
  def sendBattleEffects = synchronized {
    // Note: These are skewed by 1 so we have to adjust manually
    for(battle <- battleHist if battle._2 == currentFrame+1) {
      println("PC: battle effect "+battle._1 +" before frame#"+(currentFrame+1)+"!")
      battleController ! PlayBattleMessage(battle,this)
    }
  }

/*
  var currentIndex = 0
  var currentSeq = 1
  def pollState: State = {
    if (currentIndex >= hist.length) return EmptyState
    currentFrame += 1

    val currentState = hist(currentIndex)
    for(battle <- battleHist if battle._2 == currentFrame) {
      println("Playback: battle effect "+battle._1 +" on frame#"+currentFrame+"!")
      BattleController ! SendBattleEffect(battle._1,this)
    }

    if(currentSeq < currentState.len) currentSeq += 1
    else { currentIndex += 1; currentSeq = 1 }

    currentState.state
  }
*/

  /** Needs to be synchronized otherwise insertions may happen out of order */
  def ++= (iter : Iterable[StateSeq]): Unit = synchronized {
    val flatmap = iter.flatMap(_.toSeq)
    println("PC:Adding "+flatmap.toList.length+" states to "+this)
    hist ++= flatmap
  }
  
  def addBattle (battle: (BattleEffect, Int)): Unit = synchronized {
    println("PC: Adding battle effect "+battle._1+" for frame#"+battle._2+", currentFrame="+currentFrame+"!")
    if(battle._2 == currentFrame+1) {
      println("PC: Sending battleeffect NOW!")
      battleController ! PlayBattleMessage(battle,this)
    }
    battleHist += battle
  }
}