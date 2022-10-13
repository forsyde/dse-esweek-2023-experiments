import scala.sys.process._
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters._

import $file.generate_platform

import $ivy.`io.github.forsyde:forsyde-io-java-core:0.5.16`
import $ivy.`io.github.forsyde:forsyde-io-java-sdf3:0.5.16`
import $ivy.`org.scala-lang.modules::scala-xml:2.0.0`

import forsyde.io.java.drivers.ForSyDeModelHandler
import forsyde.io.java.core.ForSyDeSystemGraph
import forsyde.io.java.sdf3.drivers.ForSyDeSDF3Driver
import forsyde.io.java.typed.viewers.moc.sdf.SDFActor
import forsyde.io.java.typed.viewers.impl.InstrumentedExecutable
import forsyde.io.java.typed.viewers.platform.GenericProcessingModule
import forsyde.io.java.typed.viewers.platform.InstrumentedProcessingModule
import forsyde.io.java.typed.viewers.platform.GenericMemoryModule


val modelHandler = new ForSyDeModelHandler(new ForSyDeSDF3Driver())

val actorRange = 2 to 10
val coreRange = 2 to 8
val maxNumExperiments = 100
def experiments(actors: Int)(cores: Int) = 1 to Math.min(maxNumExperiments, actors * cores)

val sdf3Gen = os.pwd / "sdf3" / "build" / "release" / "Linux" / "bin" / "sdf3generate-sdf"

def getDeSyDePlatformInput(model: ForSyDeSystemGraph): String = {
    var cores = Buffer[(GenericProcessingModule, Long)]()
    var xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    xmlString += "<platform name=\"demo_platform\">\n"
    model.vertexSet().forEach(vv => {
        GenericProcessingModule.safeCast(vv).ifPresent(cpu => {
            var memory = 0L
            model.outgoingEdgesOf(vv).stream().map(e => model.getEdgeTarget(e)).flatMap(out => GenericMemoryModule.safeCast(out).stream()).forEach(mem => {
                memory += mem.getSpaceInBits()
            })
            cores +:= (cpu, memory)
        })
    })
    cores.foreach(tuple => {
        val cpu = tuple._1
        val maxSize = tuple._2
        xmlString += s"  <processor model=\"${cpu.getIdentifier()}\" number=\"1\">\n"
        xmlString += s"    <mode name=\"default\" cycle=\"${1}\" mem=\"${maxSize}\" dynPower=\"0\" staticPower=\"1\" area=\"0\" monetary=\"0\"/>\n"
        xmlString += s"  </processor>\n"
    })
    xmlString += s"""<interconnect>
                    |    <TDMA_bus name="TDMA-bus" x-dimension="${cores.size}" flitSize="32" tdma_slots="${cores.size}" maxSlotsPerProc="${cores.size}">
                    |        <mode name="default" cycleLength="1"/>
                    |    </TDMA_bus>
                    |</interconnect>""".stripMargin
    xmlString + "</platform>"
}

def getSdfGenerationInput(nActors: Int, vecSum: Int): String = {
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<sdf3 xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance" version="1.0" type="sdf" xsi:noNamespaceSchemaLocation="https://www.es.ele.tue.nl/sdf3/xsd/sdf3-sdf.xsd">
       |    <settings type='generate'>
       |        <graph>
       |            <actors nr='$nActors'/>
       |            <degree avg='${nActors / 2}' var='${nActors / 2 - 1}' min='1' max='$nActors'/>
       |            <rate avg='${nActors / 2}' var='${nActors / 2 - 1}' min='1' max='$nActors' repetitionVectorSum='$vecSum'/>
       |            <initialTokens prop='0'/>
       |            <structure stronglyConnected='false' acyclic='true' multigraph='true'/>
       |        </graph>
       |        <graphProperties>
       |            <procs nrTypes='1' mapChance='0.5'/>
       |            <execTime avg='${nActors * 10}' var='$nActors' min='1' max='${nActors * 10}'/>
       |            <stateSize avg='2048' var='1024' min='512' max='8192'/>
       |            <tokenSize avg='128' var='32' min='8' max='512'/>
       |        </graphProperties>
       |    </settings>
       |</sdf3>""".stripMargin
}

def generateDSEConfig(expPath: os.Path): String = 
    s"""# input file or path. Multiple paths allowed.
       |inputs=${expPath.toString()}/sdfs/
       |inputs=${expPath.toString()}/xmls/
       |output=${expPath.toString()}/desyde_output/
       |log-file=${expPath.toString()}/desyde_output/output.log
       |output-file-type=ALL_OUT
       |output-print-frequency=ALL_SOL
       |log-level=INFO
       |log-level=DEBUG
       |[presolver]
       |model=ONE_PROC_MAPPINGS
       |search=ALL
       |[dse]
       |model=SDF_PR_ONLINE
       |search=OPTIMIZE
       |criteria=THROUGHPUT
       |criteria=POWER
       |# search timeout. 0 means infinite. If two values are provided, the
       |# first one specifies the timeout for the first solution, and the
       |# second one for all solutions.
       |timeout=60000
       |timeout=60000
       |threads=1
       |noGoodDepth=75
       |luby_scale=100
       |th_prop=MCR""".stripMargin


def computeWCETTable(model: ForSyDeSystemGraph, timeMultiplier: Long = 1000000000L): String = {
    var string = "<WCET_table>\n"
    model.vertexSet().forEach(v => {
        SDFActor.safeCast(v).flatMap(actor => InstrumentedExecutable.safeCast(actor)).ifPresent(iactor => {
            string += s"  <mapping task_type=\"${iactor.getIdentifier()}\">\n"
            model.vertexSet().forEach(vv => {
                GenericProcessingModule.safeCast(vv).flatMap(cpu => InstrumentedProcessingModule.safeCast(cpu)).ifPresent(icpu => {
                    var minWcet = Double.MaxValue
                    iactor.getOperationRequirements().forEach((opGroup, opNeeds) => {
                        icpu.getModalInstructionsPerCycle().forEach((ipcGroup, ipc) => {
                            if (opNeeds.keySet().asScala.subsetOf(ipc.keySet().asScala)) {
                                var wcet = 0.0
                                opNeeds.forEach((k, v) => wcet += v / ipc.get(k))
                                wcet = wcet / icpu.getOperatingFrequencyInHertz()
                                minWcet = Math.min(wcet, minWcet)
                            }
                        })
                    })
                    if (minWcet < Int.MaxValue) {
                        string += s"    <wcet processor=\"${icpu.getIdentifier()}\" mode=\"default\" wcet=\"${(timeMultiplier * minWcet).toLong}\"/>\n"
                    }
                })
            })
            string += "  </mapping>\n"
        })
    })
    string + "</WCET_table>"
}

@main
def generate(): Unit = {
    for (actors <- actorRange; cores <- coreRange) {
        val combinationFolder = os.pwd / "sdfComparison" / s"plat_${cores}_actors_${actors}"
        val idesydePlatform = generate_platform.makeTDMASingleBusPlatform(cores, 32L)
        val desydePlatform = getDeSyDePlatformInput(idesydePlatform)
        val sdf3HSDFGen = getSdfGenerationInput(actors, actors)
        os.makeDir.all(combinationFolder)
        for (exp <- experiments(actors)(cores)) {
            val hsdfExpFolder = combinationFolder / s"hsdf_$exp"
            os.makeDir.all(hsdfExpFolder)
            os.makeDir.all(hsdfExpFolder / "sdfs")
            os.makeDir.all(hsdfExpFolder / "xmls")
            val desydeFile = (hsdfExpFolder / "xmls" / "desyde_platform_input.xml").toNIO
            val idesydeFile = (hsdfExpFolder / "idesyde_platform_input.fiodl").toNIO
            val sdfGenFile = (hsdfExpFolder / "sdf3_hsdf_gen.xml").toNIO
            val sdfAppFile = (hsdfExpFolder / "sdfs" / "applications_input.sdf3.xml").toNIO
            val configFile = (hsdfExpFolder / "config.cfg").toNIO
            val configString = generateDSEConfig(hsdfExpFolder)
            java.nio.file.Files.writeString(desydeFile, desydePlatform, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
            java.nio.file.Files.writeString(sdfGenFile, sdf3HSDFGen, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
            java.nio.file.Files.writeString(configFile, configString, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
            // java.nio.file.Files.copy(sdfAppFile, (hsdfExpFolder / "applications_desyde_input.hsdf.xml").toNIO, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            modelHandler.writeModel(idesydePlatform, idesydeFile)
            if (!java.nio.file.Files.exists(sdfAppFile)) {
                Seq(sdf3Gen.toString(), "--settings", sdfGenFile.toString(), "--output", sdfAppFile.toString()).!
            }
            val idesydeFullInput = (hsdfExpFolder / "idesyde_input.fiodl").toNIO
            val sdfApp = modelHandler.loadModel(sdfAppFile)
            val dseProblem = sdfApp.merge(idesydePlatform)
            modelHandler.writeModel(dseProblem, idesydeFullInput)
            val wcetTableFile = (hsdfExpFolder / "xmls" / "WCETs.xml").toNIO
            val wcetTable = computeWCETTable(dseProblem)
            java.nio.file.Files.writeString(wcetTableFile, wcetTable, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
        }
    }
}