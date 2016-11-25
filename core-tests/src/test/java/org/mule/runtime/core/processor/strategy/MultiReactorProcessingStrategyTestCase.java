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
import static org.mule.runtime.core.processor.strategy.AbstractSchedulingProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.processor.strategy.MultiReactorProcessingStrategyFactory.MultiReactorProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

public class MultiReactorProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public MultiReactorProcessingStrategyTestCase(boolean reactive) {
    super(reactive);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext) {
    return new MultiReactorProcessingStrategy(() -> cpuLight, scheduler -> {
    }, muleContext);
  }

  @Override
  protected void assertSingleCpuLight() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertSingleCpuLightConcurrent() {
    assertThat(threads.size(), equalTo(2));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(2l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertMultipleCpuLight() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertSingleBlocking() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertMultipleBlocking() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertSingleCpuIntensive() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertMultipleCpuIntensive() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertMix() {
    assertEverythingOnEventLoop();
  }

  @Override
  protected void assertMix2() {
    assertEverythingOnEventLoop();
  }

  private void assertEverythingOnEventLoop() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

  @Override
  public void tx() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    expectedException.expect(DefaultMuleException.class);
    expectedException.expectMessage(equalTo(TRANSACTIONAL_ERROR_MESSAGE));
    process(flow, testEvent());
  }

}
