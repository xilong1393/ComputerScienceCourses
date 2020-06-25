package mapreduce

import scala.collection.mutable.HashMap
import akka.actor.{Actor}
import com.typesafe.config.ConfigFactory
import mapreduce.MapReduceApplication.system

class ReduceActor extends Actor {
  var remainingMappers = ConfigFactory.load.getInt("number-mappers")
  var reduceMap: HashMap[String, Int]=new HashMap[String, Int]()
  def receive = {
    case word:String=>{
      if (reduceMap.contains(word)){
        var count=reduceMap(word)
        reduceMap +=(word -> (count+1))
      }
      else{
        reduceMap += (word -> 1)
      }
    }
    case Flush =>
      remainingMappers -= 1
      if (remainingMappers == 0) {
        println(self.path + " : " + reduceMap)
        val master = system.actorSelection("akka://MapReduceApp/user/master")
		    println(context.actorSelection("../.."))
		    //context.parent!Done
        master !Done
      }
  }
}
