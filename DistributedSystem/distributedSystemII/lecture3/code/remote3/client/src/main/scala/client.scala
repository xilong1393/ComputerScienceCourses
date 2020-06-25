import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorSystem, Props, Address, AddressFromURIString, ActorLogging}
import akka.remote.routing.RemoteRouterConfig
import akka.routing.RoundRobinPool

class Joe extends Actor {
  def receive = {
    case msg: String => println(self.path.toString + " received " + msg.toString + " from " + sender.toString)
    case _ => println("Received unknown msg ")
  }
}

object Client extends App {
  val system = ActorSystem("GreetingSystem", ConfigFactory.load.getConfig("remotelookup"))

  val addresses = Seq(
    Address("akka", "GreetingSystem"),
    Address("akka", "GreetingSystem", "127.0.0.1", 25520),
    AddressFromURIString("akka://GreetingSystem@127.0.0.1:25530"))

  val routerRemote = system.actorOf(
    RemoteRouterConfig(RoundRobinPool(6), addresses).props(Props[Joe]))

  routerRemote ! "1st hello from a remote client!"
  routerRemote ! "2nd hello from a remote client!"
  routerRemote ! "3rd hello from a remote client!"
  routerRemote ! "4th hello from a remote client!"
  routerRemote ! "5th hello from a remote client!"
  routerRemote ! "6th hello from a remote client!"
  println("Client has sent 6 hellos to routerRemote")
  //Thread.sleep(30000)
  //system.terminate
}
