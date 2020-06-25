My implementation is a little bit different from the requirement.
In comparison with previous versions, the master acts as both master and mapper, the worker works as a reducer.
Operation Process:
1. go to elastic_mapreduce0 folder: sbt->run, wait for 10 seconds, the applicaton starts running
2. go to elastic_mapreduce1 folder: sbt->runMain clustering.AdditionalWorker1
3. go to elastic_mapreduce2 folder: sbt->runMain clustering.AdditionalWorker2
4. go to elastic_mapreduce3 folder: sbt->runMain clustering.AdditionalWorker3
The result with show on the first command window:
the -> 56074, a -> 27356, an -> 3405

To properly exit, ctrl + C and exit
