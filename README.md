# dse-sdf-paper-experiments

The core files are the scala ones, ending with `*.sc`. 
You need to have ammonite (java/scala in general) to be able to run the experiments.
You need to be on a linux machine and have the compiled `sdf3` toolset to able to generate the experiments.
The scripts assume:
 * that a binary `desyde` exists in this folder,
 * a self-contained jar `idesyde.jar` exist in this folder,
 * a folder `sdf3` exist in this folder containing the compiled [SDF3 toolset](https://www.es.ele.tue.nl/sdf3/).