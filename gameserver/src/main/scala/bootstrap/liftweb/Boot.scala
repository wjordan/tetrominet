package bootstrap.liftweb

import _root_.javax.servlet.http.{HttpServletRequest, Cookie}
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
    val x: List[WsEndpoint] = List(RestApi)

    x.foreach(endpoint => LiftRules.dispatch.append
              (endpoint.dispatchRules))
  }
}


/**WsEndpoint is a trait that handles the conversion from what Lift expects,
 * i.e. () => Full[XmlResponse] to () => Node */
trait WsEndpoint {
  def wsDispatchRules: PartialFunction[Req, () => Node]

  def dispatchRules: PartialFunction[Req, () => Full[XmlResponse]] = {
    new MyAdapter(wsDispatchRules)
  }

  abstract class PartialFunctionAdapter[F, T1, T2](adaptee:
  PartialFunction[F, T1]) extends PartialFunction[F, T2] {
    override def isDefinedAt(r: F) = adaptee.isDefinedAt(r)

    override def apply(r: F) = {
      converter(adaptee.apply(r))
    }

    def converter(x: T1): T2

  }

  class MyAdapter(adaptee: PartialFunction[Req, () => Node])
          extends PartialFunctionAdapter[Req, () => Node, () =>
                  Full[XmlResponse]](adaptee) {
    override def converter(x: () => Node) = {
      () => Full(XmlResponse(x()))
    }
  }
}

/**  This class defines methods that take parameters from the HTTP
  request and return XML nodes. */
class RestApi(request: Req) {
  def doGet(id: String): Node = <info3 id= {id}/>

  def version = <VERSION ver="1.0"/>

  def redirect = {
    println("Redirecting!")
    Full(RedirectResponse(S.encodeURL("/api/waitForStart")))
  }

  /** Long polling to wait until the beginning of the match. */
  def waitForStart = {
    val user = UserName.is
    println("User " + user + " is waiting to play!")
    println("Waiting list = " + GameList(user).size)


    /** Long polling delay until game is started */
    val reply = UserName.is.!?(WaitMessage)
    reply match {
      case TIMEOUT => <TIMEOUT/>
      case StartMessage(seed: Long) => <GAMESTART seed= {seed.toString}/>
      case _ => <ERROR/>
    }
  }

  def get(id: String) = {
    request.param("name").map(x => UserName.is.name = x)
    println("id=" + id + ",username=" + UserName.is)
    <Hello world="Hello!"/>
  }

  def getName(params: String, req: Req) = {
    println("Session username = " + UserName.is)
    <Name id= {UserName.is.toString}/>
  }

  def addStates = {
    //    println("seed="+request.param("seed")+",xml="+request.param("xml"))
    val xml = scala.xml.XML.loadString(request.param("xml").get)

    val user = UserName.is
    val game = user.game
    //    println("Adding states for user "+user+" to game "+game)
    game ! AddStates(user, xml)
    //    println("Added states, retrieving new states")
    val states = game !? GetOpponentStates(user)
    //    println("Retrieved states, response = "+states)
    states.asInstanceOf[Node]
  }
}

// The companion object sets up the mapping rules, e.g. /api/item/1 ->
// exampleApi.doGet(1)
object RestApi extends WsEndpoint {
  override def wsDispatchRules = {
    case req => {
      val exampleApi = new RestApi(req)
      req match
      {
        case Req("api" :: "item" :: id :: _, _, GetRequest) => () =>
                exampleApi.doGet(id)
        case Req("api" :: "waitForStart" :: _, _, PostRequest) => () => exampleApi.waitForStart
        case Req("api" :: "addStates" :: _, _, PostRequest) => () => exampleApi.addStates
        //        case Req("api" :: "redirect" :: _, _, GetRequest) => () => exampleApi.redirect
        case Req("api" :: "version" :: _, _, GetRequest) => () => exampleApi.version
        case Req("api" :: itemid :: _, _, GetRequest) => () => exampleApi.get(itemid)
        case _ => () => exampleApi.get("XX")
      }
    }
  }

}

object UserName extends SessionVar[GameActor](new GameActor)

case class StartMessage(seed: Long)
