import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

class Joe extends Actor {
  def receive = {
    case msg: String => println("joe received " + msg)
    case _ => println("Received unknown msg ")
    case PoisonPill=>
      context.system.terminate()
  }
}

object Server extends App {
    val system = ActorSystem("GreetingSystem", ConfigFactory.load.getConfig("server"))
    val joe = system.actorOf(Props[Joe], name = "joe")
    println(joe.path)
    joe ! "a local hello"
    println("Server ready")
}
