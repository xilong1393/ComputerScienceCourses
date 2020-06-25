import java.util.concurrent.Future

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Address, AddressFromURIString, Deploy, Kill, PoisonPill, Props, Terminated}
import akka.remote.RemoteScope
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import homework6_2.system

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case object Hello
case object Test
case class WatchChild(actorRef: ActorRef)
class Child extends Actor with ActorLogging{
  def receive={
    case Hello=>
      println("Hello, there!")
  }
}
class Watcher extends Actor with ActorLogging {
  override def receive: Receive = {
    case WatchChild(child)=>
      context.watch(child)
      println("I'm watching "+child)
    case Terminated(ref)=>
      println(s"the actor i'm watch $ref is stopped!")
  }
}

object homework6_2 extends  App {
  val system = ActorSystem("GreetingSystem")
  val select = system.actorSelection("akka://GreetingSystem@127.0.0.1:25520/user/joe")
  val fchild=select.resolveOne(Duration(2, "second"))
  val child=Await.result(fchild,Duration(2, "second"))
  child ! "hi, how are you!"
  val watcher = system.actorOf(Props[Watcher], name = "watcher")
  watcher!WatchChild(child)
  //child!PoisonPill
  //system.terminate()
}
