package bootstrap.liftweb

import _root_.javax.servlet.http.HttpServletRequest
import _root_.scala.actors.Actor._
import _root_.scala.actors._
import _root_.scala.xml._
import scala.collection.mutable.HashMap
import scala.collection.Set
import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import Helpers._
import net.liftweb.http
import ScalaTetris.net._

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot {
  def boot {
    LiftRules.addDispatchAfter{
      case r @ RequestState("api" :: _, "", _) => new RestAPI(r).index
    }
  }
}


object UserName extends SessionVar[GameActor](new GameActor)

case class StartMessage(seed: Long)

class RestAPI(request: RequestState) {

  def index(): Can[LiftResponse] = {
    request match {
      case RequestState("api" :: "waitForStart" :: _ , _, PostRequest) => waitForStart
      case RequestState("api" :: "addStates" :: _ , _, PostRequest) => addStates
      case RequestState("api" :: "redirect" :: _ , _, GetRequest) => redirect
      case RequestState("api" :: itemid :: _ , _, GetRequest) => get(itemid)
      case _ => get("XX")
    }
  }

  def redirect = {
    println("Redirecting!")
    Full(RedirectResponse(S.encodeURL("/api/waitForStart")))
  }
  
  /** Long polling to wait until the beginning of the match. */
  def waitForStart = {
    val user = UserName.is
    println("User "+user+" is waiting to play!")
    println("Waiting list = "+GameList(user).size)
    

    /** Long polling delay until game is started */
    val reply = UserName.is.!?(WaitMessage)
    Full(XmlResponse(reply match {
      case TIMEOUT => <TIMEOUT/>
      case StartMessage(seed:Long) => <GAMESTART seed={seed.toString}/>
      case _ => <ERROR/>
    }))
  }

  def get(id: String) = {
    request.param("name").map(x => UserName.is.name = x )
    println("id="+id+",username="+UserName.is)
    Full(XmlResponse(<Hello world="Hello!"/>))
  }

  def getName(params : String, req : RequestState) = {
    println("Session username = "+UserName.is)
    Full(XmlResponse(<Name id={UserName.is.toString}/>))
  }

  def addStates = {
//    println("seed="+request.param("seed")+",xml="+request.param("xml"))
    val xml = scala.xml.XML.loadString(request.param("xml").get)

    val user = UserName.is
    val game = user.game
//    println("Adding states for user "+user+" to game "+game)
    game ! AddStates(user,xml)
//    println("Added states, retrieving new states")
    val states = game !? GetOpponentStates(user)
//    println("Retrieved states, response = "+states)
    Full(XmlResponse(states.asInstanceOf[Node]))
  }
}