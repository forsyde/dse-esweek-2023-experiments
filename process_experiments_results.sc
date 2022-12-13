import $file.generate_experiments

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.temporal.ChronoUnit


val idesydeBenchmark = Paths.get("idesyde_benchmark.csv")
val idesydeScalBenchmark = Paths.get("idesyde_scal_benchmark.csv")
val desydeBenchmark = Paths.get("desyde_benchmark.csv")

val desydeDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val idesydeDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss:SSS")

def recompute_idesyde_1(): Unit = {
    if (!Files.exists(idesydeBenchmark)) {
        Files.createFile(idesydeBenchmark)
    }
    Files.writeString(
        idesydeBenchmark,
        "plat, actors, svr, exp, start, first, runtime_first, last, runtime_last, stop, runtime\n",
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
    )
    for (
        actors <- generate_experiments.actorRange1;
        svr <- generate_experiments.svrMultiplicationRange1;
        cores <- generate_experiments.coreRange1;
        exp <- generate_experiments.experiments(actors)(cores)
    ) {
       // println((actors, cores, exp).toString())
       val outFile = (os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).toInt}"/ s"plat_${cores}" / s"exp_$exp" / "idesyde_output.log").toNIO
       if (Files.exists(outFile)) {
           var startingTime = LocalDateTime.now()
           var firstTime = LocalDateTime.now()
           var lastTime = LocalDateTime.now()
           var endTime = LocalDateTime.now()
           Files.lines(outFile).forEach(l => {
                if (l.contains("decision model(s) and explorer(s) chosen")) {
                    startingTime = LocalDateTime.parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
                } else if (l.contains("solution_0")) {
                   // println(l)
                    firstTime = LocalDateTime.parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
                    lastTime = LocalDateTime.parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
                } else if (l.contains("writing solution")) {
                    lastTime = LocalDateTime.parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
                } else if (l.contains("Finished exploration")) {
                    endTime = LocalDateTime.parse(l.subSequence(0, 23), idesydeDateTimeFormatter)
                }
           })
           val runtimeFirst = ChronoUnit.MILLIS.between(startingTime, firstTime)
           val runtimeLast = ChronoUnit.MILLIS.between(startingTime, lastTime)
           val runtime = ChronoUnit.MILLIS.between(startingTime, endTime)
           Files.writeString(
             idesydeBenchmark,
             s"$cores, $actors, ${(svr * 100).toInt}, $exp, $startingTime, $firstTime, $runtimeFirst, $lastTime, $runtimeLast, $endTime, $runtime\n",
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
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
    )
    for (
        actors <- generate_experiments.actorRange1;
        svr <- generate_experiments.svrMultiplicationRange1;
        cores <- generate_experiments.coreRange1;
        exp <- generate_experiments.experiments(actors)(cores)
    ) {
       // println((actors, cores, exp).toString())
       val outFile = (os.pwd / "sdfComparison" / s"actors_${actors}" / s"svr_${(svr * 100).toInt}"/ s"plat_${cores}" / s"exp_$exp" / "output.log").toNIO
       if (Files.exists(outFile)) {
           var startingTime = LocalDateTime.now()
           var firstTime = LocalDateTime.now()
           var lastTime = LocalDateTime.now()
           var endTime = LocalDateTime.now()
           Files.lines(outFile).forEach(l => {
                if (l.contains("PRESOLVER executing full model - finding 1")) {
                    startingTime = LocalDateTime.parse(l.subSequence(0, 23), desydeDateTimeFormatter)
                } else if (l.contains("PRESOLVER executing full model - finding 2")) {
                   // println(l)
                    firstTime = LocalDateTime.parse(l.subSequence(0, 23), desydeDateTimeFormatter)
                    lastTime = LocalDateTime.parse(l.subSequence(0, 23), desydeDateTimeFormatter)
                } else if (l.contains("solution found so far.")) {
                    lastTime = LocalDateTime.parse(l.subSequence(0, 23), desydeDateTimeFormatter)
                } else if (l.contains("End of exploration")) {
                    endTime = LocalDateTime.parse(l.subSequence(0, 23), desydeDateTimeFormatter)
                }
           })
           val runtimeFirst = ChronoUnit.MILLIS.between(startingTime, firstTime)
           val runtimeLast = ChronoUnit.MILLIS.between(startingTime, lastTime)
           val runtime = ChronoUnit.MILLIS.between(startingTime, endTime)
           Files.writeString(
             desydeBenchmark,
             s"$cores, $actors, ${(svr * 100).toInt}, $exp, $startingTime, $firstTime, $runtimeFirst, $lastTime, $runtimeLast, $endTime, $runtime\n",
             StandardOpenOption.APPEND
           )
       }
    }
}

@main
def recomputeAll(): Unit = {
    recompute_idesyde_1()
    recompute_desyde_1()
}