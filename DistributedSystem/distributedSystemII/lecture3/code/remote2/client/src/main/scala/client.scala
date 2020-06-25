import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorSystem, Props, AddressFromURIString, ActorLogging}
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
    AddressFromURIString("akka://GreetingSystem@140.192.39.187:25520"),
    AddressFromURIString("akka://GreetingSystem@140.192.37.109:25520")
)

  val routerRemote = system.actorOf(
    RemoteRouterConfig(RoundRobinPool(4), addresses).props(Props[Joe]))

  routerRemote ! "1st hello from a remote client!"
  routerRemote ! "2nd hello from a remote client!"
  routerRemote ! "3rd hello from a remote client!"
  routerRemote ! "4th hello from a remote client!"
  println("Client has sent 4 hellos to routerRemote")
  Thread.sleep(30000)
  system.terminate
}
