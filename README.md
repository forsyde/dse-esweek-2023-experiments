# Paper 45 experiments

1. The core files are the scala ones, ending with `*.sc`. 
2. You need to have ammonite (java/scala in general) to be able to run the experiments. You can install it by following the [Coursier](https://get-coursier.io/) guidelines.
3. You need to be on a linux machine and have the compiled `sdf3` toolset to be able to generate the experiments. Sadly that is not negotiable. `sdf3` misbehaves badly in other OSes or does not compile at all.

The scripts assume:
 * a self-contained jar `idesyde.jar` exist in this folder (provided);
 * a folder `sdf3` exists in this folder containing the compiled [SDF3 toolset](https://www.es.ele.tue.nl/sdf3/).

# Running the experiments

With all installed, you first need to generate the experiments:

    amm generate_experiments generate

And then you can run (in parallel if you have enough resources):

    # indirect case studies
    amm run_experiments indirect_case_studies 
    # time-out of 5 days (hardness)
    amm run_experiments hardness_evaluation 
    # time-out of 30 minutes (anytime)
    amm run_experiments anytime_evaluation 