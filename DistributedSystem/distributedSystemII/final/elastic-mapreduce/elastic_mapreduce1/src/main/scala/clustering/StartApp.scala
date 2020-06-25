package clustering

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{ConfigFactory}

object StartApp{
   def main(args: Array[String]): Unit = {
    //start the application with one master and two workers, later we can add workers to the cluster as many as we want
    val master = createNode(2551, "master", Props[Master], "master")
    createNode(2552, "worker", Props[Worker], "worker")
    createNode(2553, "worker", Props[Worker], "worker")
    //wait for some time for the cluster to converge
    Thread.sleep(10000)

    val url1="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg580.txt"
    val url2="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg968.txt"
    val url3="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg766.txt"
    val url4="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg700.txt"
    master ! ProcessUrls(Array[String](url1,url2,url3,url4))
  }

  def createNode(port: Int, role: String, props: Props, actorName: String): ActorRef = {
    val config = ConfigFactory.parseString(
      s"""
         |akka.cluster.roles = ["$role"]
         |akka.remote.artery.canonical.port = $port
       """.stripMargin)
      .withFallback(ConfigFactory.load("clustering/application.conf"))

    val system = ActorSystem("ElasticCluster", config)
    system.actorOf(props, actorName)
  }
}