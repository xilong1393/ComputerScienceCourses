import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.collection.mutable

case class SendClockwiseTokenTo(ref2:ActorRef,ref3:ActorRef)
case class SendCounterClockwiseTokenTo(ref3:ActorRef,ref2:ActorRef)
case class SendMarker(map:Map[String,ActorRef])
class ActorWithTokens extends Actor {
  var clockwiseTokenCount=0
  var counterClockwiseTokenCount=0
  var isFirstMarker=true
  //I'm using actor name to identify different actor, as actor is unique
  var transientMessageMap:Map[String,mutable.Queue[String]]=Map("first"->mutable.Queue[String](),"second"->mutable.Queue[String](),"third"->mutable.Queue[String]())
  var stateMap:Map[String,Int]=Map("clockwiseTokenCount"->0,"counterClockwiseTokenCount"->0)
  var startRecordingMap:Map[String,Boolean]=Map("first"->false,"second"->false,"third"->false)

  def receive = {
    case SendClockwiseTokenTo(ref2,ref3) =>
      val selfName=context.self.path.name.toString()
      val senderName=context.sender().path.name.toString()
      if(senderName!="deadLetters"&&startRecordingMap(senderName)){
          var q=transientMessageMap(selfName)
          q+=senderName
          transientMessageMap+=(selfName->q)
      }
      clockwiseTokenCount+=1
      println("ClockwiseToken reached "+context.self.path.name+", its ClockwiseTokenCount is "+clockwiseTokenCount)
      Thread.sleep(500)
      ref2!SendClockwiseTokenTo(ref3,context.self)
    case SendCounterClockwiseTokenTo(ref3,ref2) =>
      val selfName=context.self.path.name.toString()
      val senderName=context.sender().path.name.toString()
      if(senderName!="deadLetters"&&startRecordingMap(senderName)){
        var q=transientMessageMap(selfName)
        q+=senderName
        transientMessageMap+=(selfName->q)
      }
      counterClockwiseTokenCount+=1
      println("CounterClockwiseToken reached "+selfName+", its CounterClockwiseTokenCount is "+counterClockwiseTokenCount)
      Thread.sleep(500)
      ref3!SendCounterClockwiseTokenTo(ref2,context.self)
    case SendMarker(map)=>
      val senderName=context.sender().path.name.toString()
      val selfName=context.self.path.name.toString()
      if(isFirstMarker){
        isFirstMarker=false
        stateMap+=("clockwiseTokenCount"->clockwiseTokenCount,"counterClockwiseTokenCount"->counterClockwiseTokenCount)
        println("=====Node "+ selfName +" received its first marker from "+senderName+", the state of the tokenCount is: "+stateMap)
        //open to the message channel and multiCast the marker to all other nodes
        startRecordingMap.keys.foreach{i=>
          if(i!=selfName){
            if(i!=senderName){
              startRecordingMap+=(i->true)
              println("=====message recording channel from "+i+" to "+selfName+" is open=====")
            }
          val act=map(i)
          act!SendMarker(map)
        }}
        Thread.sleep(2000)
      }else{
        println("marker arrived at Node "+selfName+", not for the first time")
        startRecordingMap+=(senderName->false)
        Thread.sleep(1000)
        println("=====channel from "+senderName+ " to "+selfName+" is closed!=====The transient message queue for this channel is "+transientMessageMap(selfName))
      }
  }
}

object Problem2 extends App {
  val system = ActorSystem("PassTokens")
  val first = system.actorOf(Props[ActorWithTokens], name = "first")
  val second = system.actorOf(Props[ActorWithTokens], name = "second")
  val third = system.actorOf(Props[ActorWithTokens], name = "third")
  var map:Map[String,ActorRef]=Map("first"->first,"second"->second,"third"->third)
  println(first.path)
  println(second.path)
  println(third.path)
  println("Server ready")
  first ! SendClockwiseTokenTo(second,third)
  first ! SendCounterClockwiseTokenTo(third,second)
  Thread.sleep(1000)
  first ! SendMarker(map)
}
