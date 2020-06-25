package mapreduce

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorSystem, Address, AddressFromURIString, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy}
import akka.remote.routing.RemoteRouterConfig
import akka.routing.{Broadcast, ConsistentHashingPool, RoundRobinGroup, RoundRobinPool}
import com.typesafe.config.ConfigFactory

class MasterActor extends Actor {
  val numberMappers  = ConfigFactory.load.getInt("number-mappers")
  val numberReducers  = ConfigFactory.load.getInt("number-reducers")
  var pending = numberReducers
  val strategy = AllForOneStrategy() {
    case e => Restart
  }

  val addresses1 = Seq(
    Address("akka", "CountSystem"))
    //Address("akka", "CountSystem", "127.0.0.1", 25540),
    //AddressFromURIString("akka://CountSystem@127.0.0.1:25550"))
    //
  val reduceActors = context.actorOf(RemoteRouterConfig(ConsistentHashingPool(numberReducers), addresses1).props(Props[ReduceActor]))

  val addresses2 = Seq(
    Address("akka", "CountSystem"))
//    Address("akka", "CountSystem", "127.0.0.1", 25520),
//    AddressFromURIString("akka://CountSystem@127.0.0.1:25530"))
  val mapActors = context.actorOf(
    RemoteRouterConfig(RoundRobinPool(numberMappers,supervisorStrategy = strategy), addresses2).props(Props(classOf[MapActor],reduceActors)))
    //val mapActors = context.actorOf(RoundRobinGroup(paths).props(classOf[MapActor],reduceActors),"rounter2")
  def receive = {
    case Msg(title,url) =>
      mapActors ! Msg(title,url)
    case Flush =>
      mapActors ! Broadcast(Flush)
    case Done =>
      pending -= 1
      if (pending == 0){
        context.system.terminate
        println("terminated!")
      }
  }
}
