import $file.generate_experiments

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.temporal.ChronoUnit
import scala.collection.mutable.Buffer
import scala.util.matching.Regex
import java.time.Duration

val idesydeBenchmark = Paths.get("idesyde_benchmark.csv")
val idesydeScalBenchmark = Paths.get("idesyde_scal_benchmark.csv")
val desydeBenchmark = Paths.get("desyde_benchmark.csv")

val desydeDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val idesydeDateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss:SSS")

val combinationsExp1Names = Vector(
  "RaJp",
  "SoSuRa",
  "SoSuJp",
  "SoRaJp",
  "SuRaJp",
  "SoSuRaJp",
  "So",
  "Syn"
)

def recompute_idesyde_2(): Unit = {
  if (!Files.exists(idesydeBenchmark)) {
    Files.createFile(idesydeBenchmark)
  }
  Files.writeString(
    idesydeBenchmark,
    "plat, actors, svr, exp, start, first, runtime_first, last, runtime_last, stop, runtime, mean_time_to_improvement\n",
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING
  )
  for (
    actors <- generate_experiments.actorRange1;
    svr <- generate_experiments.svrMultiplicationRange1;
    cores <- generate_experiments.coreRange1;
    exp <- 1 to generate_experiments.dataPointsPerTuple
  ) {
    // println((actors, cores, exp).toString())
    val outFile =
      (os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).toInt}" / s"plat_${cores}" / s"exp_$exp" / "idesyde_output.log").toNIO
    if (Files.exists(outFile)) {
      var startingTime = LocalDateTime.now()
      var firstTime = LocalDateTime.now()
      var lastTime = LocalDateTime.now()
      var endTime = LocalDateTime.now()
      var timesToImprovement = Buffer[Long]()
      Files
        .lines(outFile)
        .forEach(l => {
          if (l.contains("decision model(s) and explorer(s) chosen")) {
            startingTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
          } else if (l.contains("solution_0")) {
            // println(l)
            firstTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
            lastTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
          } else if (l.contains("writing solution")) {
            val now = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
            timesToImprovement += ChronoUnit.MILLIS.between(lastTime, now)
            lastTime = now
          } else if (l.contains("Finished exploration")) {
            endTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
          }
        })
      val runtimeFirst = ChronoUnit.MILLIS.between(startingTime, firstTime)
      val runtimeLast = ChronoUnit.MILLIS.between(startingTime, lastTime)
      val runtime = ChronoUnit.MILLIS.between(startingTime, endTime)
      val meanTimeToImprovement =
        if (!timesToImprovement.isEmpty) then
          timesToImprovement.sum / timesToImprovement.size
        else 0L
      Files.writeString(
        idesydeBenchmark,
        s"$cores, $actors, ${(svr * 100).toInt}, $exp, $startingTime, $firstTime, $runtimeFirst, $lastTime, $runtimeLast, $endTime, $runtime, $meanTimeToImprovement\n",
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND
      )
    }
  }
}

def recompute_desyde_1(): Unit = {
  if (!Files.exists(desydeBenchmark)) {
    Files.createFile(desydeBenchmark)
  }
  Files.writeString(
    desydeBenchmark,
    "plat, actors, svr, exp, start, first, runtime_first, last, runtime_last, stop, runtime\n",
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING
  )
  for (
    actors <- generate_experiments.actorRange1;
    svr <- generate_experiments.svrMultiplicationRange1;
    cores <- generate_experiments.coreRange1;
    exp <- 1 to generate_experiments.dataPointsPerTuple
  ) {
    // println((actors, cores, exp).toString())
    val outFile =
      (os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).toInt}" / s"plat_${cores}" / s"exp_$exp" / "desyde_output.log").toNIO
    if (Files.exists(outFile)) {
      var startingTime = LocalDateTime.now()
      var firstTime = LocalDateTime.now()
      var lastTime = LocalDateTime.now()
      var endTime = LocalDateTime.now()
      Files
        .lines(outFile)
        .forEach(l => {
          if (l.contains("PRESOLVER executing full model - finding 1")) {
            startingTime =
              LocalDateTime.parse(l.subSequence(0, 19), desydeDateTimeFormatter)
          } else if (l.contains("PRESOLVER executing full model - finding 2")) {
            // println(l)
            firstTime =
              LocalDateTime.parse(l.subSequence(0, 19), desydeDateTimeFormatter)
            lastTime =
              LocalDateTime.parse(l.subSequence(0, 19), desydeDateTimeFormatter)
          } else if (l.contains("solution found so far.")) {
            lastTime =
              LocalDateTime.parse(l.subSequence(0, 19), desydeDateTimeFormatter)
          } else if (l.contains("End of exploration")) {
            endTime =
              LocalDateTime.parse(l.subSequence(0, 19), desydeDateTimeFormatter)
          }
        })
      val runtimeFirst = ChronoUnit.MILLIS.between(startingTime, firstTime)
      val runtimeLast = ChronoUnit.MILLIS.between(startingTime, lastTime)
      val runtime = ChronoUnit.MILLIS.between(startingTime, endTime)
      Files.writeString(
        desydeBenchmark,
        s"$cores, $actors, ${(svr * 100).toInt}, $exp, $startingTime, $firstTime, $runtimeFirst, $lastTime, $runtimeLast, $endTime, $runtime\n",
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND
      )
    }
  }
}

val solsPattern =
  "nUsedPEs = ([0-9]*), globalInvThroughput = ([0-9]*) / ([0-9]*)".r

private def formatDuration(d: Duration): String = f"${d.getSeconds() / 3600}%01d:${(d.getSeconds() % 3600)/60}%02d:${d.getSeconds() % 60}%02d:${d.getNano() / 1000000}"

@main
def recompute_idesyde_3_table(): Unit = {
  for ((comb, i) <- generate_experiments.combinationsExp1.zipWithIndex) {
    val aggName = comb.map(_.split("\\.").head).reduce(_ + "_" + _)
    val expFolder = os.pwd / "caseStudies" / aggName
    val outFile = (expFolder / "idesyde_output.log").toNIO
    if (Files.exists(outFile)) {
      var startingTime = LocalDateTime.now()
      var firstTime = LocalDateTime.now()
      var lastTime = LocalDateTime.now()
      var endTime = LocalDateTime.now()
      var isTimeOut = false
      var bestSolutions = Buffer[(Duration, Int, Double)]()
      Files
        .lines(outFile)
        .forEach(l => {
          if (l.contains("decision model(s) and explorer(s) chosen")) {
            startingTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
          } else if (l.contains("solution: ")) {
            lastTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
            for (m <- solsPattern.findAllMatchIn(l)) {
              bestSolutions.addOne(
                (
                  Duration.between(startingTime, lastTime),
                  m.group(1).toInt,
                  1000 * m.group(2).toDouble / m.group(3).toDouble
                )
              )
            }
            // now take away domninants
          } else if (l.contains("Finished exploration")) {
            endTime = LocalDateTime
              .parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
            isTimeOut = ChronoUnit.SECONDS.between(startingTime, endTime) >= 3600*6
          }
        })
      val firstSol = bestSolutions.minByOption((time, _, _) => time).getOrElse((Duration.ZERO, 0, 0.0))
      val bestForCores = bestSolutions.minByOption((time, n, l) => (n, l, time)).getOrElse((Duration.ZERO, 0, 0.0))
      val bestForTh = bestSolutions.minByOption((time, n, l) => (l, n, time)).getOrElse((Duration.ZERO, 0, 0.0))
      println(s" & ${formatDuration(firstSol._1)} & ${formatDuration(bestForCores._1)} & ${formatDuration(bestForTh._1)} & \\\\")
      println(s" ${combinationsExp1Names(i)}${if (isTimeOut) then "" else "$^*$"} & ${firstSol._2} cores & ${bestForCores._2} cores & ${bestForTh._2} cores & \\\\")
      println(f" & ${1000.0 / firstSol._3}%4.2f exe/ms & ${ 1000.0 / bestForCores._3}%4.2f exe/ms & ${1000.0 / bestForTh._3}%4.2f exe/ms & \\\\")
      println(" & & & & \\\\")
    }
  }
}

@main
def recomputeAll(): Unit = {
  recompute_idesyde_2()
  recompute_desyde_1()
}
