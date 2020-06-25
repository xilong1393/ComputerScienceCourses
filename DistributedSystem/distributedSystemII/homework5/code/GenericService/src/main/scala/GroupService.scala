import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable
import scala.collection.mutable.HashMap

class GroupServer(val myNodeID: Int, val numNodes: Int, groupStoreServers: Seq[ActorRef], burstSize: Int) extends Actor{
  val generator = new scala.util.Random
  val cellstore = new KVClient(groupStoreServers)
  val dirtycells = new HashMap[BigInt, Any]
  val localWeight: Int = 70
  val log = Logging(context.system, this)

  var stats = new Stats
  var allocated: Int = 0
  var endpoints: Option[Seq[ActorRef]] = None

  var isFree: Boolean = true
  var groupID: BigInt = BigInt(-1)
  var receivedMulticast:Int=0

  def receive() = {
    case Prime() =>
      join
    case Command() =>
      command
      statistics(sender)
    case View(e) =>
      endpoints = Some(e)
    case Multicast() =>
      stats.receivedMulticast+=1
  }

  private def leave()= {
    if(!isFree){
      val leaveSet = directRead(groupID)
      if(!leaveSet.isEmpty){
        var set = leaveSet.get
        if(!set.isEmpty){
          set.remove(self)
          directWrite(groupID,set)
          isFree=true
          groupID=BigInt(-1)
        }else
          stats.misses+=1
      }
    }
    else{
      stats.misses+=1
    }
  }

  private def join() = {
    val toKey=chooseRandomGroup
    var toSet=directRead(toKey)
      if(isFree){
        if(!toSet.isEmpty)
        {
          var cur=toSet.get
          cur.add(self)
        }
        else
        {
          var cur=new mutable.HashSet[ActorRef]
          cur.add(self)
          directWrite(toKey,cur)
        }
        isFree=false
        groupID=toKey
      }else
        stats.misses+=1
  }

  def multiBroadcast() = {
    //multicast only when the current actor is in a group
    if (isFree)
      stats.misses += 1
    else
    {
      var set = directRead(groupID)
      if (!set.isEmpty)
        {
          for (s <- set.get)
          {
            //don't broadcast to oneself
            if (s != self)
              s!Multicast()
          }
        }
    }
  }

  private def command() = {
    val cmd = generator.nextInt(3)
    if(cmd==0)
    {
      stats.join+=1
      join
    }
    else if(cmd==1)
    {
      stats.leave+=1
      leave
    }
    else
    {
      stats.multicast+=1
      multiBroadcast
    }
  }

  private def statistics(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      stats.groupID=groupID
      master ! BurstAck(myNodeID, stats)
      //stats = new Stats
    }
  }

  private def chooseRandomGroup(): BigInt =
  {
    //randomly choose a groupID from range [0,99]
    val rdm =generator.nextInt(numNodes)
    BigInt(rdm)
  }

  private def directRead(key: BigInt): Option[mutable.HashSet[ActorRef]] = {
    val result = cellstore.directRead(key)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[mutable.HashSet[ActorRef]])
  }

  private def directWrite(key: BigInt, value: Any): Option[mutable.HashSet[ActorRef]] = {
    val result = cellstore.directWrite(key, value)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[mutable.HashSet[ActorRef]])
  }
}
object GroupServer{
  def props(myNodeID: Int, numNodes: Int, groupStoreServers: Seq[ActorRef], burstSize: Int): Props = {
    Props(classOf[GroupServer], myNodeID, numNodes, groupStoreServers, burstSize)
  }
}