/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.runtime.core.api.scheduler.ThreadType.IO;
import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

public class SynchronousProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public SynchronousProcessingStrategyTestCase(boolean reactive) {
    super(reactive);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext) {
    return SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  protected void assertSingleCpuLight() {
    assertSynchronous(1);
  }

  @Override
  protected void assertSingleCpuLightConcurrent() {
    assertSynchronous(2);
  }

  @Override
  protected void assertMultipleCpuLight() {
    assertSynchronous(1);
  }

  @Override
  protected void assertSingleBlocking() {
    assertSynchronous(1);
  }

  @Override
  protected void assertMultipleBlocking() {
    assertSynchronous(1);
  }

  @Override
  protected void assertSingleCpuIntensive() {
    assertSynchronous(1);
  }

  @Override
  protected void assertMultipleCpuIntensive() {
    assertSynchronous(1);
  }

  @Override
  protected void assertMix() {
    assertSynchronous(1);
  }

  @Override
  protected void assertMix2() {
    assertSynchronous(1);
  }

  @Override
  public void tx() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    process(flow, testEvent());

    assertSynchronous(1);
  }

  private void assertSynchronous(int concurrency) {
    assertThat(threads.size(), equalTo(concurrency));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

}
