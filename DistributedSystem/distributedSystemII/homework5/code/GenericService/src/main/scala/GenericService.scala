import scala.collection.mutable.HashMap

import akka.actor.{Actor, ActorSystem, ActorRef, Props}
import akka.event.Logging

class GenericCell(var prev: BigInt, var next: BigInt)

/**
 * GenericService is an example app service for the actor-based KVStore/KVClient.
 * This one stores Generic Cell objects in the KVStore.  Each app server allocates new
 * GenericCells (allocCell), writes them, and reads them randomly with consistency
 * checking (touchCell).  The allocCell and touchCell commands use direct reads
 * and writes to bypass the client cache.  Keeps a running set of Stats for each burst.
 *
 * @param myNodeID sequence number of this actor/server in the app tier
 * @param numNodes total number of servers in the app tier
 * @param storeServers the ActorRefs of the KVStore servers
 * @param burstSize number of commands per burst
 */

class GenericServer (val myNodeID: Int, val numNodes: Int, storeServers: Seq[ActorRef], burstSize: Int) extends Actor {
  val generator = new scala.util.Random
  val cellstore = new KVClient(storeServers)
  val dirtycells = new HashMap[BigInt, Any]
  val localWeight: Int = 70
  val log = Logging(context.system, this)

  var stats = new Stats
  var allocated: Int = 0
  var endpoints: Option[Seq[ActorRef]] = None


  def receive() = {
      case Prime() =>
        allocCell
      case Command() =>
        statistics(sender)
        command
      case View(e) =>
        endpoints = Some(e)
  }

  private def command() = {
    val sample = generator.nextInt(100)
    if (sample < 50) {
      allocCell
    } else {
      touchCell
    }
  }

  private def statistics(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      master ! BurstAck(myNodeID, stats)
      stats = new Stats
    }
  }

  private def allocCell() = {
    val key = chooseEmptyCell
    var cell = directRead(key)
    assert(cell.isEmpty)
    val r = new GenericCell(0, 1)
    stats.allocated += 1
    directWrite(key, r)
  }

  private def chooseEmptyCell(): BigInt =
  {
    allocated = allocated + 1
    cellstore.hashForKey(myNodeID, allocated)
  }

  private def touchCell() = {
    stats.touches += 1
    val key = chooseActiveCell
    val cell = directRead(key)
    if (cell.isEmpty) {
      stats.misses += 1
    } else {
      val r = cell.get
      if (r.next != r.prev + 1) {
        stats.errors += 1
        r.prev = 0
        r.next = 1
      } else {
        r.next += 1
        r.prev += 1
      }
      directWrite(key, r)
    }
  }

  private def chooseActiveCell(): BigInt = {
    val chosenNodeID =
      if (generator.nextInt(100) <= localWeight)
        myNodeID
      else
        generator.nextInt(numNodes - 1)

    val cellSeq = generator.nextInt(allocated)
    cellstore.hashForKey(chosenNodeID, cellSeq)
  }

  private def directRead(key: BigInt): Option[GenericCell] = {
    val result = cellstore.directRead(key)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[GenericCell])
  }

  private def directWrite(key: BigInt, value: GenericCell): Option[GenericCell] = {
    val result = cellstore.directWrite(key, value)
    if (result.isEmpty) None else
      Some(result.get.asInstanceOf[GenericCell])
  }

/*
  private def push(dirtyset: HashMap[BigInt, Any]) = {
    cellstore.push(dirtyset)
  }
 */
}

object GenericServer {
  def props(myNodeID: Int, numNodes: Int, storeServers: Seq[ActorRef], burstSize: Int): Props = {
    Props(classOf[GenericServer], myNodeID, numNodes, storeServers, burstSize)
  }
}
