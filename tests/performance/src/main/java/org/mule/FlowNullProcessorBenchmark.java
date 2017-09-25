/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.Collections.singletonList;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;

public class FlowNullProcessorBenchmark extends AbstractFlowBenchmark {

  @Override
  protected List<Processor> getMessageProcessors() {
    return singletonList(nullProcessor);
  }

  @Override
  protected int getStreamIterations() {
    return 1000;
  }

  @Benchmark
  public CoreEvent processor() throws MuleException {
    return nullProcessor.process(createEvent(flow));
  }

}
