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

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 64

##############################################
# CPU Intensive Processor
##############################################
echo "org.mule.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowCPULightlProcessorBenchmark -t 64

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
echo "org.mule.FlowMixedAProcessorBenchmark (Mix)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.FlowMixedAProcessorBenchmark -t 64
