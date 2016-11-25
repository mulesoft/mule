/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.runtime.core.api.scheduler.ThreadType.IO;
import static org.mule.runtime.core.processor.strategy.AbstractSchedulingProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.processor.strategy.ProactorProcessingStrategyFactory.ProactorProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

public class ProactorProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public ProactorProcessingStrategyTestCase(boolean reactive) {
    super(reactive);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext) {
    return new ProactorProcessingStrategy(() -> cpuLight, () -> blocking, () -> cpuIntensive,
                                          scheduler -> {
                                          },
                                          scheduler -> currentThread().getName().startsWith(scheduler.getThreadType().name()),
                                          muleContext);
  }

  @Override
  protected void assertSingleCpuLight() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertSingleCpuLightConcurrent() {
    assertThat(threads.size(), equalTo(2));
  }

  @Override
  protected void assertMultipleCpuLight() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertSingleBlocking() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertMultipleBlocking() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertSingleCpuIntensive() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertMultipleCpuIntensive() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(0l));
  }

  @Override
  protected void assertMix() {
    assertThat(threads.size(), equalTo(3));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(1l));

  }

  @Override
  protected void assertMix2() {

    assertThat(threads.size(), allOf(greaterThanOrEqualTo(3), lessThanOrEqualTo(5)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), allOf(
                                                                                                 greaterThanOrEqualTo(1l),
                                                                                                 lessThanOrEqualTo(3l)));
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
