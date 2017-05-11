#!/bin/sh

##############################################
# Null Processor
##############################################
echo "org.mule.FlowNullProcessorBenchmark (Null Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 64
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowNullProcessorBenchmark -t 128

##############################################
# CPU Light Processor
##############################################
echo "org.mule.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 64

##############################################
# CPU Intensive Processor
##############################################
echo "org.mule.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightProcessorBenchmark -t 64

##############################################
# Blocking Processor
##############################################
echo "org.mule.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowBlockingProcessorBenchmark -t 64

##############################################
# Mixed Processors A
##############################################
echo "org.mule.FlowMixedAProcessorBenchmark (Mix)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 64

##############################################
# Mixed Processors A
##############################################
echo "org.mule.FlowMixedAProcessorBenchmark (Mix)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 64


##############################################
# Mixed Processors A
##############################################
echo "org.mule.FlowMixedAProcessorBenchmark (Mix A)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 64

##############################################
# Mixed Processors B
##############################################
echo "org.mule.FlowMixedBProcessorBenchmark (Mix B)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedBProcessorBenchmark -t 64


##############################################
# Mixed Processors C
##############################################
echo "org.mule.FlowMixedCProcessorBenchmark (Mix C)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedCProcessorBenchmark -t 64
