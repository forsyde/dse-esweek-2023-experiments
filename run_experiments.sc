import scala.sys.process._

import $file.generate_experiments
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.nio.file.StandardOpenOption

val desydeBin = (os.pwd / "desyde").toString()
val idesydeBin = (os.pwd / "idesyde.jar").toString()

val idesydeBenchmark = Paths.get("idesyde_benchmark.csv")
val desydeBenchmark = Paths.get("desyde_benchmark.csv")

@main
def runAll(): Unit = {
    if (!Files.exists(idesydeBenchmark)) {
        Files.createFile(idesydeBenchmark)
        Files.writeString(idesydeBenchmark, "plat, actors, exp, start, stop, runtime\n", StandardOpenOption.APPEND)
    }
    if (!Files.exists(desydeBenchmark)) {
        Files.createFile(desydeBenchmark)
        Files.writeString(desydeBenchmark, "plat, actors, exp, start, stop, runtime\n", StandardOpenOption.APPEND)
    }
    for (actors <- generate_experiments.actorRange; cores <- generate_experiments.coreRange; exp <- generate_experiments.experiments(actors)(cores)) {
        println(s"-- Solving combination $actors, $cores, $exp")
        val expFolder = os.pwd / "sdfComparison" / s"plat_${cores}_actors_${actors}" / s"hsdf_$exp"
        val desydeOutput = expFolder / "desyde_output"
        val desydeOutputOut = expFolder / "desyde_output" / "out"
        val idesydeOutput = expFolder / "idesyde_output"
        java.nio.file.Files.createDirectories(desydeOutput.toNIO)
        java.nio.file.Files.createDirectories(desydeOutputOut.toNIO)
        java.nio.file.Files.createDirectories(idesydeOutput.toNIO)
        if (!Files.exists((expFolder / "idesyde_output.log").toNIO)) {
            if (!Files.lines((expFolder / "idesyde_output.log").toNIO).anyMatch(l => l.contains("Finished exploration"))) {
                val beforeIdesyde = LocalDateTime.now()
                (Seq(
                    "java", "-jar",
                    idesydeBin,
                    "-o", idesydeOutput.toString(),
                    "--solutions-limit", "100",
                    "--log", (expFolder / "idesyde_output.log").toString(),
                    (expFolder / "idesyde_input.fiodl").toString()
                    )).!
                }
                val afterIdesyde = LocalDateTime.now()
                val elapsed = ChronoUnit.MILLIS.between(beforeIdesyde, afterIdesyde)
                Files.writeString(idesydeBenchmark, s"$cores, $actors, $exp, $beforeIdesyde, $afterIdesyde, $elapsed\n", StandardOpenOption.APPEND)
        }
        if (!Files.exists((expFolder / "desyde_output" / "output.log").toNIO)) {
            if (!Files.lines((expFolder / "desyde_output" / "output.log").toNIO).anyMatch(l => l.contains("End of exploration"))) {
                val beforeDesyde = LocalDateTime.now()
                (Seq(
                    desydeBin,
                    "--config", (expFolder / "config.cfg").toString()
                )).!
                val afterDesyde = LocalDateTime.now()
                val elapsedDesyde = ChronoUnit.MILLIS.between(beforeDesyde, afterDesyde)
                Files.writeString(desydeBenchmark, s"$cores, $actors, $exp, $beforeDesyde, $afterDesyde, $elapsedDesyde\n", StandardOpenOption.APPEND)
            }
        }
    }
}