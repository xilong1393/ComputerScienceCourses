package client

import akka.actor.{Actor, Props, Address, Deploy}
import akka.routing.{Broadcast, RoundRobinPool}
import akka.remote.RemoteScope
import akka.remote.routing.RemoteRouterConfig

import com.typesafe.config.ConfigFactory

import common._

class MasterActor extends Actor {

  val numberMappers  = ConfigFactory.load.getInt("number-mappers")
  val numberReducers  = ConfigFactory.load.getInt("number-reducers")

  var pending = numberReducers

  val addresses = Seq(
    Address("akka", "MapReduceServer", "127.0.0.1", 25510),
    Address("akka", "MapReduceServer", "127.0.0.1", 25520)
  )


  var reduceActors = List[String]()
  for (i <- 0 until numberReducers)
    reduceActors = context.actorOf(Props[ReduceActor].withDeploy(Deploy(scope = RemoteScope(addresses(i % addresses.length))))).path.toString::reduceActors


  val mapActors = context.actorOf(RemoteRouterConfig(RoundRobinPool(numberMappers), addresses).props(Props(classOf[MapActor], reduceActors)))


  def receive = {
    case msg: Book =>
      mapActors ! msg
    case Flush =>
      mapActors ! Broadcast(Flush)
    case Done =>
      println("Received Done from" + sender)
      pending -= 1
      if (pending == 0)
        context.system.terminate
  }
}
