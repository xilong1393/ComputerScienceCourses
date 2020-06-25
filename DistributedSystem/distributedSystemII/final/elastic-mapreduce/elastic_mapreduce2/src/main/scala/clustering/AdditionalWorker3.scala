package clustering

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AdditionalWorker3{
  val config = ConfigFactory.parseString(
    s"""
       |akka.cluster.roles = ["worker"]
       |akka.remote.artery.canonical.port = 2556
       """.stripMargin)
    .withFallback(ConfigFactory.load("clustering/application.conf"))

  val system = ActorSystem("ElasticCluster", config)
  def main(args: Array[String]): Unit = {
    system.actorOf(Props[Worker],"worker")
  }
}