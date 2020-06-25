package clustering

import akka.actor.{Actor, ActorLogging, ReceiveTimeout}

import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.language.postfixOps

class Aggregator extends Actor with ActorLogging {
  context.setReceiveTimeout(3 seconds)
  var resultMap: HashMap[String, Int]= HashMap[String, Int]()

  override def receive: Receive = {
    case ProcessMapResult(map:HashMap[String,Int]) =>
      for((k,v)<-map){if (resultMap.contains(k)) resultMap(k) += map(k) else resultMap(k) = map(k)}
    case ReceiveTimeout =>
      log.info(s"RESULT MAP: $resultMap")
      context.setReceiveTimeout(Duration.Undefined)
  }
}