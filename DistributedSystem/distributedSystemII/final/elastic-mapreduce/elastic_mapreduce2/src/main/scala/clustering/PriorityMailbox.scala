package clustering

import akka.actor.ActorSystem
import akka.cluster.ClusterEvent.MemberEvent
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

//reorder the actor order in the mailbox when an actor is newly joint so that the work can be allocate to it
class PriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox(
    PriorityGenerator {
      case _: MemberEvent => 0
      case _ => 1
    }
  )
