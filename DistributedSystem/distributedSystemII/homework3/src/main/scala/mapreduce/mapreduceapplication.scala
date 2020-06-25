package mapreduce
import akka.actor.{ActorSystem, Props}

object MapReduceApplication extends App {
  val system = ActorSystem("MapReduceApp")
  val master = system.actorOf(Props[MasterActor], name = "master")

  val url1="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg580.txt"
  val url2="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg968.txt"
  val url3="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg766.txt"
  val url4="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg700.txt"
  val url5="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg699.txt"
  val url6="http://reed.cs.depaul.edu/lperkovic/gutenberg/pg963.txt"

  //just to make the output display better, I shortened the title.
  master ! Msg("Pickwick Papers",url1)
  master ! Msg("Life And Adventures",url2)
  master ! Msg("David Copperfield",url3)
  master ! Msg("Curiosity Shop",url4)
  master ! Msg("History of England",url5)
  master ! Msg("Little Dorrit",url6)
  master ! Flush
}
