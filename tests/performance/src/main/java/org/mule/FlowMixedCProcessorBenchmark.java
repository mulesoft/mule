/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.OutputTimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FlowMixedCProcessorBenchmark extends AbstractFlowBenchmark {

  @Override
  protected List<Processor> getMessageProcessors() {
    List<Processor> processors = new ArrayList<>();
    processors.add(cpuLightProcessor);
    processors.add(cpuLightProcessor);
    processors.add(cpuIntensiveProcessor);
    processors.add(cpuLightProcessor);
    processors.add(cpuLightProcessor);
    processors.add(cpuLightProcessor);
    return processors;
  }

  @Override
  protected int getStreamIterations() {
    return 50;
  }

}
