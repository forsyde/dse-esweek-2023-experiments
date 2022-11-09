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

val desydeBin = (os.pwd / "desyde").toString()
val idesydeBin = (os.pwd / "idesyde.jar").toString()

val idesydeBenchmark = Paths.get("idesyde_benchmark.csv")
val idesydeScalBenchmark = Paths.get("idesyde_scal_benchmark.csv")
val desydeBenchmark = Paths.get("desyde_benchmark.csv")

@main
def evaluation_1_idesyde(): Unit = {
  if (!Files.exists(idesydeBenchmark)) {
    Files.createFile(idesydeBenchmark)
    Files.writeString(
      idesydeBenchmark,
      "plat, actors, exp, start, first, runtime_first, stop, runtime\n",
      StandardOpenOption.APPEND
    )
  }
  for (
    actors <- generate_experiments.actorRange1;
    cores <- generate_experiments.coreRange1;
    exp <- generate_experiments.experiments(actors)(cores)
  ) {
    println(s"-- Solving combination $actors, $cores, $exp")
    val expFolder =
      os.pwd / "sdfComparison" / s"plat_${cores}_actors_${actors}" / s"hsdf_$exp"
    val idesydeOutput = expFolder / "idesyde_output"
    java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
    if (
      !Files.exists((expFolder / "idesyde_output.log").toNIO) || Files
        .lines((expFolder / "idesyde_output.log").toNIO)
        .noneMatch(l => l.contains("Finished exploration"))
    ) {
      val beforeIdesyde = LocalDateTime.now()
      (
        Seq(
          "java",
          "-jar",
          idesydeBin,
          "-v",
          "DEBUG",
          "--decision-model",
          "ChocoSDFToSChedTileHW",
          "-o",
          idesydeOutput.toString(),
          "--log",
          (expFolder / "idesyde_output.log").toString(),
          (expFolder / "idesyde_input.fiodl").toString()
        )
      ).!
      val attrs = Files.readAttributes(
        (idesydeOutput / "solution_0.fiodl").toNIO,
        classOf[BasicFileAttributes]
      )
      val afterIdesyde = LocalDateTime.now()
      val elapsed = ChronoUnit.MILLIS.between(beforeIdesyde, afterIdesyde)
      val firstFound = LocalDateTime.ofInstant(
        attrs.lastModifiedTime().toInstant(),
        ZoneId.systemDefault()
      )
      val firstElapsed = ChronoUnit.MILLIS.between(beforeIdesyde, firstFound)
      Files.writeString(
        idesydeBenchmark,
        s"$cores, $actors, $exp, $beforeIdesyde, $firstFound, $firstElapsed, $afterIdesyde, $elapsed\n",
        StandardOpenOption.APPEND
      )
    }
  }
}

val desyedDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@main
def evaluation_1_desyde(): Unit = {
  if (!Files.exists(desydeBenchmark)) {
    Files.createFile(desydeBenchmark)
    Files.writeString(
      desydeBenchmark,
      "plat, actors, exp, start, stop, runtime, first, runtime_first\n",
      StandardOpenOption.APPEND
    )
  }
  for (
    actors <- generate_experiments.actorRange1;
    cores <- generate_experiments.coreRange1;
    exp <- generate_experiments.experiments(actors)(cores)
  ) {
    println(s"-- Solving combination A $actors, P $cores, E $exp")
    val expFolder =
      os.pwd / "sdfComparison" / s"plat_${cores}_actors_${actors}" / s"hsdf_$exp"
    val desydeOutput = expFolder / "desyde_output"
    val desydeOutputOut = expFolder / "desyde_output" / "out"
    java.nio.file.Files.createDirectories(desydeOutput.toNIO)
    java.nio.file.Files.createDirectories(desydeOutputOut.toNIO)
    if (
      !Files.exists((expFolder / "desyde_output" / "output.log").toNIO) || Files
        .lines((expFolder / "desyde_output" / "output.log").toNIO)
        .noneMatch(l => l.contains("End of exploration"))
    ) {
      val beforeDesyde = LocalDateTime.now()
      (Seq(
        desydeBin,
        "--config",
        (expFolder / "config.cfg").toString()
      )).!
      val afterDesyde = LocalDateTime.now()
      val firstTimeLine = Files.lines((expFolder / "desyde_output" / "output.log").toNIO).filter(s => s.contains("PRESOLVER executing full model - finding 2")).findAny()
      val firstTime = firstTimeLine.map(s => s.subSequence(0, 19)).map(s => LocalDateTime.parse(s, desyedDateTimeFormatter)).orElse(afterDesyde)
      val elapsedDesyde = ChronoUnit.MILLIS.between(beforeDesyde, afterDesyde)
      val elapsedDesydeFirst = ChronoUnit.MILLIS.between(beforeDesyde, firstTime)
      Files.writeString(
        desydeBenchmark,
        s"$cores, $actors, $exp, $beforeDesyde, $afterDesyde, $elapsedDesyde, $firstTime, $elapsedDesydeFirst\n",
        StandardOpenOption.APPEND
      )
    }
  }
}

@main
def evaluation_2_idesyde(): Unit = {
  if (!Files.exists(idesydeScalBenchmark)) {
    Files.createFile(idesydeScalBenchmark)
    Files.writeString(
      idesydeScalBenchmark,
      "plat, actors, exp, start, first, runtime_first, stop, runtime\n",
      StandardOpenOption.APPEND
    )
  }
  for (
    cores <- generate_experiments.coreRange2;
    actors <- generate_experiments.actorRange2
  ) {
    println(s"-- Solving combination A $actors, P $cores")
    val expFolder =
      os.pwd / "sdfScalability" / s"actors_${actors}" / s"plat_${cores}"
    val idesydeOutput = expFolder / "idesyde_output"
    java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
    if (
      !Files.exists((expFolder / "idesyde_output.log").toNIO) || Files
        .lines((expFolder / "idesyde_output.log").toNIO)
        .noneMatch(l => l.contains("Finished exploration"))
    ) {
      val beforeIdesyde = LocalDateTime.now()
      (
        Seq(
          "java",
          "-Xmx24G",
          "-jar",
          idesydeBin,
          "-v",
          "DEBUG",
          "--decision-model",
          "ChocoSDFToSChedTileHW",
          "--exploration-timeout",
          "3600",
          "-o",
          idesydeOutput.toString(),
          "--log",
          (expFolder / "idesyde_output.log").toString(),
          (expFolder / "idesyde_input.fiodl").toString()
        )
      ).!
      val attrs = Files.readAttributes(
        (idesydeOutput / "solution_0.fiodl").toNIO,
        classOf[BasicFileAttributes]
      )
      val afterIdesyde = LocalDateTime.now()
      val elapsed = ChronoUnit.MILLIS.between(beforeIdesyde, afterIdesyde)
      val firstFound = LocalDateTime.ofInstant(
        attrs.lastModifiedTime().toInstant(),
        ZoneId.systemDefault()
      )
      val firstElapsed = ChronoUnit.MILLIS.between(beforeIdesyde, firstFound)
      Files.writeString(
        idesydeBenchmark,
        s"$cores, $actors, $beforeIdesyde, $firstFound, $firstElapsed, $afterIdesyde, $elapsed\n",
        StandardOpenOption.APPEND
      )
    }
  }
}
