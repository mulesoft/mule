/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.FlowNullProcessorBenchmark;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareWorkQueueProcessingStrategyFactory;

import org.junit.Test;

public class FlowProcessBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  private static final String PROCESSING_STRATEGY_PARAM = "processingStrategyFactory";

  @Test
  public void processStreamOf1000FlowDefault() {
    runAndAssertBenchmark(FlowNullProcessorBenchmark.class, "processSourceStream", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {
                                           TransactionAwareWorkQueueProcessingStrategyFactory.class.getCanonicalName()}),
                          9, MILLISECONDS, 7800000);
  }


  @Test
  public void processStreamOf1000FlowSynchronous() {
    runAndAssertBenchmark(FlowNullProcessorBenchmark.class, "processSourceStream", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {BlockingProcessingStrategyFactory.class.getCanonicalName()}),
                          9, MILLISECONDS, 7800000);
  }


}
