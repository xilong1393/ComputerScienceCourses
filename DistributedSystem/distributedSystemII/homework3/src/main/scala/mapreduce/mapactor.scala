package mapreduce

import akka.actor.{Actor, ActorRef}
import akka.routing.Broadcast
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope

import scala.io.Source
class MapActor(reduceActor: ActorRef) extends Actor {
  val STOP_WORDS_LIST = List("a", "am", "an", "and", "are", "as", "at", "be",
    "do", "go", "if", "in", "is", "it", "of", "on", "the", "to")
  override def preRestart(reason:Throwable,message:Option[Any])={
    println("mapactor restarting!")
  }
  override def preStart = {
    println("mapactor starting!")
  }
  override def postStop = {
    println("mapactor stopped!")
  }
  def receive = {
    case Msg(title,url) =>
      //try{
        println(s"$self begin mapping")
        val content=Source.fromURL(url).mkString.trim()
        //throw new Exception("hello, im exception!")
        process(content,title)
      //}catch {
      //  case e: Exception => println("caught an exception while reading content from "+url)
      //}

    case Flush =>
      reduceActor!Broadcast(Flush)
  }

  def process(content: String, title:String) = {
    println(s"$self begin processing!")
    for (word <- content.split("[\\p{Punct}\\s]+"))
      //if (word.equals(word.toUpperCase()))
	    //  reduceActor ! ConsistentHashableEnvelope(message = Word(word, title), hashKey = word)
      if(STOP_WORDS_LIST.contains(word))
        reduceActor ! ConsistentHashableEnvelope(message = word, hashKey = word)
  }
}