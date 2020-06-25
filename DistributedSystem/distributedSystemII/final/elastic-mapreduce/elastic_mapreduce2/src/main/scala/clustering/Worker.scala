package clustering

import akka.actor.{Actor, ActorLogging}
import scala.collection.mutable.HashMap

class Worker extends Actor with ActorLogging {
  val STOP_WORDS_LIST = List("a", "the", "an")

  override def receive: Receive = {
    case ProcessLine(line, aggregator) =>{
      log.info(s"Processing: $line")
      val map =new HashMap[String,Int]
      val strArr=line.split("[\\p{Punct}\\s]+")
      for(k<-strArr){
        if(STOP_WORDS_LIST.contains(k)){
          if (map.contains(k))
            map(k) += 1
          else
            map(k) = 1
        }
      }
      aggregator ! ProcessMapResult(map)
    }
  }
}
