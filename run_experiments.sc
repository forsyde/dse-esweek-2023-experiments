import scala.sys.process._

import $file.generate_experiments
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.collection.mutable.Queue

val desydeBin = (os.pwd / "desyde").toString()
val idesydeBin = (os.pwd / "idesyde.jar").toString()

@main
def hardness_evaluation(): Unit = {
  // if (!Files.exists(idesydeBenchmark)) {
  //   Files.createFile(idesydeBenchmark)
  //   Files.writeString(
  //     idesydeBenchmark,
  //     "plat, actors, exp, start, first, runtime_first, stop, runtime\n",
  //     StandardOpenOption.APPEND
  //   )
  // }
  var queue = Queue((generate_experiments.actorRange1.head, generate_experiments.coreRange1.head, 1, generate_experiments.svrMultiplicationRange1.head))
  var solved = Queue[(Int, Int, Int, Double)]()
  while (!queue.isEmpty) {
    val (actors, cores, exp, svr) = queue.dequeue()
    println(s"-- Solving combination $actors, $cores, $svr, $exp")
    val expFolder =
      os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).ceil.toInt}" / s"plat_${cores}" / s"exp_$exp"
    val idesydeOutput = expFolder / "idesyde_output"
    java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
    var elapsed = 0L
    if (
      !Files.exists((expFolder / "idesyde_output.log").toNIO) || Files
        .lines((expFolder / "idesyde_output.log").toNIO)
        .noneMatch(l => l.contains("Finished exploration"))
    ) {
      val beforeIdesyde = LocalDateTime.now()
      (
        Seq(
          "java",
          "-Xmx16G",
          "-jar",
          idesydeBin,
          "-v",
          "DEBUG",
          "--exploration-timeout",
          "432000",
          "--decision-model",
          "ChocoSDFToSChedTileHW2",
          "-o",
          idesydeOutput.toString(),
          "--log",
          (expFolder / "idesyde_output.log").toString(),
          (expFolder / "idesyde_input.fiodl").toString()
        )
      ).!
      val afterIdesyde = LocalDateTime.now()
      elapsed = ChronoUnit.DAYS.between(beforeIdesyde, afterIdesyde)
    }
    if (elapsed < 5L) {
      // one extra actor
      if (actors < generate_experiments.actorRange1.max) {
        val next = (generate_experiments.actorRange1.filter(_ > actors).head, cores, exp, svr)
        if (!solved.contains(next) && !queue.contains(next)) {
          queue += next
        }
      }
      if (cores < generate_experiments.coreRange1.max) {
        val next = (actors, generate_experiments.actorRange1.filter(_ > cores).head, exp, svr)
        if (!solved.contains(next) && !queue.contains(next)) {
          queue += next
        }
      }
      if (exp < generate_experiments.dataPointsPerTuple) {
        val next = (actors, cores, exp + 1, svr)
        if (!solved.contains(next) && !queue.contains(next)) {
          queue += next
        }
      }
      if (svr < generate_experiments.svrMultiplicationRange1.max) {
        val next = (actors, cores, exp, generate_experiments.svrMultiplicationRange1.filter(_ > svr).head)
        if (!solved.contains(next) && !queue.contains(next)) {
          queue += next
        }
      }
    }
    solved = solved.addOne((actors, cores, exp, svr))
  }
  // for (
  //   exp <- 1 to generate_experiments.dataPointsPerTuple;
  //   cores <- generate_experiments.coreRange1;
  //   actors <- generate_experiments.actorRange1;
  //   svr <- generate_experiments.svrMultiplicationRange1
  // ) {
  //   println(s"-- Solving combination $actors, $cores, $svr, $exp")
  //   val expFolder =
  //     os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).ceil.toInt}" / s"plat_${cores}" / s"exp_$exp"
  //   val idesydeOutput = expFolder / "idesyde_output"
  //   java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
  //   if (
  //     !Files.exists((expFolder / "idesyde_output.log").toNIO) || Files
  //       .lines((expFolder / "idesyde_output.log").toNIO)
  //       .noneMatch(l => l.contains("Finished exploration"))
  //   ) {
  //     // val beforeIdesyde = LocalDateTime.now()
  //     (
  //       Seq(
  //         "java",
  //         "-Xmx16G",
  //         "-jar",
  //         idesydeBin,
  //         "-v",
  //         "DEBUG",
  //         "--exploration-timeout",
  //         "432000",
  //         "--decision-model",
  //         "ChocoSDFToSChedTileHW2",
  //         "-o",
  //         idesydeOutput.toString(),
  //         "--log",
  //         (expFolder / "idesyde_output.log").toString(),
  //         (expFolder / "idesyde_input.fiodl").toString()
  //       )
  //     ).!
  //     // val afterIdesyde = LocalDateTime.now()
  //     // val elapsed = ChronoUnit.DAYS.between(beforeIdesyde, afterIdesyde)
  //     // val attrs = Files.readAttributes(
  //     //   (idesydeOutput / "solution_0.fiodl").toNIO,
  //     //   classOf[BasicFileAttributes]
  //     // )
  //     // val afterIdesyde = LocalDateTime.now()
  //     // val elapsed = ChronoUnit.MILLIS.between(beforeIdesyde, afterIdesyde)
  //     // val firstFound = LocalDateTime.ofInstant(
  //     //   attrs.lastModifiedTime().toInstant(),
  //     //   ZoneId.systemDefault()
  //     // )
  //     // val firstElapsed = ChronoUnit.MILLIS.between(beforeIdesyde, firstFound)
  //     // Files.writeString(
  //     //   idesydeBenchmark,
  //     //   s"$cores, $actors, $exp, $beforeIdesyde, $firstFound, $firstElapsed, $afterIdesyde, $elapsed\n",
  //     //   StandardOpenOption.APPEND
  //     // )
  //   }
  // }
}

@main
def evaluation_1_desyde(): Unit = {
  // if (!Files.exists(desydeBenchmark)) {
  //   Files.createFile(desydeBenchmark)
  //   Files.writeString(
  //     desydeBenchmark,
  //     "plat, actors, exp, start, stop, runtime, first, runtime_first\n",
  //     StandardOpenOption.APPEND
  //   )
  // }
  for (
    exp <- 1 to generate_experiments.dataPointsPerTuple;
    actors <- generate_experiments.actorRange1;
    svr <- generate_experiments.svrMultiplicationRange1;
    cores <- generate_experiments.coreRange1
  ) {
    println(s"-- Solving combination A $actors, SVR $svr, P $cores, E $exp")
    val expFolder =
      os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).ceil.toInt}" / s"plat_${cores}" / s"exp_$exp"
    val desydeOutput = expFolder / "desyde_output"
    val desydeOutputOut = expFolder / "desyde_output" / "out"
    java.nio.file.Files.createDirectories(desydeOutput.toNIO)
    java.nio.file.Files.createDirectories(desydeOutputOut.toNIO)
    if (
      !Files.exists((expFolder / "desyde_output.log").toNIO) || Files
        .lines((expFolder / "desyde_output.log").toNIO)
        .noneMatch(l => l.contains("End of exploration"))
    ) {
      val beforeDesyde = LocalDateTime.now()
      (Seq(
        desydeBin,
        "--config",
        (expFolder / "config.cfg").toString()
      )).!
      // val afterDesyde = LocalDateTime.now()
      // val firstTimeLine = Files.lines((expFolder / "desyde_output" / "output.log").toNIO).filter(s => s.contains("PRESOLVER executing full model - finding 2")).findAny()
      // val firstTime = firstTimeLine.map(s => s.subSequence(0, 19)).map(s => LocalDateTime.parse(s, desyedDateTimeFormatter)).orElse(afterDesyde)
      // val elapsedDesyde = ChronoUnit.MILLIS.between(beforeDesyde, afterDesyde)
      // val elapsedDesydeFirst = ChronoUnit.MILLIS.between(beforeDesyde, firstTime)
      // Files.writeString(
      //   desydeBenchmark,
      //   s"$cores, $actors, $exp, $beforeDesyde, $afterDesyde, $elapsedDesyde, $firstTime, $elapsedDesydeFirst\n",
      //   StandardOpenOption.APPEND
      // )
    }
  }
}

@main
def anytime_evaluation(): Unit = {
  // if (!Files.exists(idesydeScalBenchmark)) {
  //   Files.createFile(idesydeScalBenchmark)
  //   Files.writeString(
  //     idesydeScalBenchmark,
  //     "plat, actors, exp, start, first, runtime_first, stop, runtime\n",
  //     StandardOpenOption.APPEND
  //   )
  // }
  for (
    svr <- generate_experiments.svrMultiplicationRange2;
    exp <- 1 to generate_experiments.dataPointsPerTuple;
    cores <- generate_experiments.coreRange2;
    actors <- generate_experiments.actorRange2
  ) {
    println(s"-- Solving combination A $actors, SVR $svr, P $cores, EXP $exp")
    val expFolder =
      os.pwd / "sdfScalability" / s"actors_${actors}" / s"svr_${(svr * 100).ceil.toInt}" / s"plat_${cores}" / s"exp_$exp"
    val idesydeOutput = expFolder / "idesyde_output"
    java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
    if (
      !Files.exists((expFolder / "idesyde_output.log").toNIO) || Files
        .lines((expFolder / "idesyde_output.log").toNIO)
        .noneMatch(l => l.contains("Finished exploration"))
    ) {
      (
        Seq(
          "java",
          "-Xmx16G",
          "-jar",
          idesydeBin,
          "-v",
          "DEBUG",
          "--decision-model",
          "ChocoSDFToSChedTileHW2",
          "--exploration-timeout",
          "1800",
          "-o",
          idesydeOutput.toString(),
          "--log",
          (expFolder / "idesyde_output.log").toString(),
          (expFolder / "idesyde_input.fiodl").toString()
        )
      ).!
    }
  }
}

@main
def indirect_case_studies(): Unit = {
  for (
    comb <- generate_experiments.combinationsExp1
  ) {
    val aggName = comb.map(_.split("\\.").head).reduce(_ + "_" + _)
    println(s"-- Solving combination $aggName")
    val expFolder = os.pwd / "caseStudies" / aggName
    val idesydeOutput = expFolder / "idesyde_output"
    java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
    if (
      !Files.exists((expFolder / "idesyde_output.log").toNIO) || Files
        .lines((expFolder / "idesyde_output.log").toNIO)
        .noneMatch(l => l.contains("Finished exploration"))
    ) {
      (
        Seq(
          "java",
          "-Xmx24G",
          "-jar",
          idesydeBin,
          "-v",
          "DEBUG",
          "--decision-model",
          "ChocoSDFToSChedTileHW2",
          "--exploration-timeout",
          "21600",
          "-o",
          idesydeOutput.toString(),
          "--log",
          (expFolder / "idesyde_output.log").toString(),
          (expFolder / "idesyde_input.fiodl").toString()
        )
      ).!
    }
  }
}
