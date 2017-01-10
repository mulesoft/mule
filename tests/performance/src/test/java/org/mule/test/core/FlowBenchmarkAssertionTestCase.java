/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.EventBenchmark;
import org.mule.FlowBenchmark;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;

import java.util.Collections;

import org.junit.Test;

public class FlowBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  private static final String PROCESSING_STRATEGY_PARAM = "processingStrategyFactory";

  @Test
  public void processSourceDefault() {
    runAndAssertBenchmark(FlowBenchmark.class, "processSource", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {DefaultFlowProcessingStrategyFactory.class.getCanonicalName()}),
                          50, MICROSECONDS, 16000);
  }

  @Test
  public void processSourceSynchronous() {
    runAndAssertBenchmark(FlowBenchmark.class, "processSource", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {SynchronousProcessingStrategyFactory.class.getCanonicalName()}),
                          5, MICROSECONDS, 5800);
  }

  @Test
  public void processFlowDefault() {
    runAndAssertBenchmark(FlowBenchmark.class, "processFlow", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {DefaultFlowProcessingStrategyFactory.class.getCanonicalName()}),
                          50, MICROSECONDS, 19000);
  }

  @Test
  public void processFlowSynchronous() {
    runAndAssertBenchmark(FlowBenchmark.class, "processFlow", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {SynchronousProcessingStrategyFactory.class.getCanonicalName()}),
                          6, MICROSECONDS, 8000);
  }

}
