package clustering

import akka.actor.{Actor, ActorLogging, ActorRef, Address, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.pattern.pipe
import akka.util.Timeout
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.Random

class Master extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(3 seconds)

  val cluster = Cluster(context.system)
  var workers: Map[Address, ActorRef] = Map()
  var pendingRemoval: Map[Address, ActorRef] = Map()

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive =
    handleClusterEvents
      .orElse(handleWorkerRegistration)
      .orElse(handleJob)

  def handleClusterEvents: Receive = {
    case MemberUp(member) if member.hasRole("worker") =>
      println(s"worker Member is up: ${member.address}")
      if (pendingRemoval.contains(member.address)) {
        pendingRemoval = pendingRemoval - member.address
      } else {
        val workerSelection = context.actorSelection(s"${member.address}/user/worker")
        workerSelection.resolveOne().map(ref => (member.address, ref)).pipeTo(self)
      }
    case MemberUp(member) if member.hasRole("master") =>
      println(s"master is up: ${member.address}")

    case UnreachableMember(member) if member.hasRole("worker") =>
      log.info(s"worker Member detected as unreachable: ${member.address}")
      val workerOption = workers.get(member.address)
      workerOption.foreach { ref =>
        pendingRemoval = pendingRemoval + (member.address -> ref)
      }
    case MemberRemoved(member, previousStatus) =>
      log.info(s"Member ${member.address} removed after $previousStatus")
      workers = workers - member.address
    case m: MemberEvent =>
      log.info(s"Any other irrelevant member event: $m")
  }

  def handleWorkerRegistration: Receive = {
    case (address:Address, actorRef:ActorRef) =>
      log.info(s"Registering worker: $actorRef")
      workers =workers+(address->actorRef)
  }

  def handleJob: Receive = {
    case ProcessUrls(urls) =>
      val aggregator = context.actorOf(Props[Aggregator], "aggregator")
      for (i <- 0 until urls.length) {
        self ! ProcessUrl(urls(i),aggregator)
      }
    case ProcessUrl(url,aggregator)=>
      println(url)
      try{

        val content = Source.fromURL(url)
        content.getLines().foreach(line => {
          self!ProcessLine(line,aggregator)
        })
      }catch {
        case exception: Exception=>
          println(exception.getMessage)
          self ! ProcessUrl(url,aggregator)
      }

    case ProcessLine(line, aggregator) =>
      val workerIndex = Random.nextInt((workers -- pendingRemoval.keys).size)
      val worker: ActorRef = (workers -- pendingRemoval.keys).values.toSeq(workerIndex)
      worker ! ProcessLine(line, aggregator)
      Thread.sleep(2)
  }
}
