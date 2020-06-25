package client

import akka.actor.SupervisorStrategy.{Resume, Stop}
import akka.actor.{Actor, Address, AllForOneStrategy, Props}
import akka.routing.{Broadcast, ConsistentHashingPool, RoundRobinPool}
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.remote.routing.RemoteRouterConfig
import com.typesafe.config.ConfigFactory
import common._

class MasterActor extends Actor {

  val numberMappers  = ConfigFactory.load.getInt("number-mappers")
  val numberReducers  = ConfigFactory.load.getInt("number-reducers")

  var pending = numberReducers
  val strategy = AllForOneStrategy() {
    case e => Stop
  }
  val addresses = Seq(
    Address("akka", "MapReduceClient"),
    Address("akka", "MapReduceServer", "127.0.0.1", 25520)
  )

  def hashMapping: ConsistentHashMapping = {
    case Word(word, title) => word
  }

  val reduceActors = context.actorOf(RemoteRouterConfig(ConsistentHashingPool(numberReducers, hashMapping = hashMapping), addresses).props(Props[ReduceActor]))

  val mapActors = context.actorOf(RemoteRouterConfig(RoundRobinPool(numberMappers,supervisorStrategy = strategy), addresses).props(Props(classOf[MapActor], reduceActors)))

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
