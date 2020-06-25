package clustering

import akka.actor.ActorRef

import scala.collection.mutable.HashMap

trait Messages
case class ProcessUrl(url:String,aggregator:ActorRef) extends Messages
case class ProcessUrls(urls: Array[String]) extends Messages
case class ProcessLine(line: String, aggregator: ActorRef) extends Messages
case class ProcessMapResult(map:HashMap[String,Int]) extends Messages
