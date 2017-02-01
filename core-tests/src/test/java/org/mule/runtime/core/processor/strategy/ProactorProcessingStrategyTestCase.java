/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.core.processor.strategy.AbstractProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.strategy.ProactorProcessingStrategyFactory.ProactorProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Processing Strategies")
@Stories("Proactor Processing Strategy")
public class ProactorProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public ProactorProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new ProactorProcessingStrategy(() -> cpuLight, () -> blocking, () -> cpuIntensive, scheduler -> {
    }, Integer.MAX_VALUE, () -> blocking, DEFAULT_BUFFER_SIZE, DEFAULT_SUBSCRIBER_COUNT, DEFAULT_WAIT_STRATEGY, muleContext);
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when all processor are CPU_LIGHT then they are all exectured in a single "
      + " cpu light thread.")
  public void singleCpuLight() throws Exception {
    super.singleCpuLight();
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("When ProactorProcessingStrategy is configured, two concurrent requests may be processed by two different "
      + " cpu light threads. MULE-11132 is needed for true reactor behaviour.")
  public void singleCpuLightConcurrent() throws Exception {
    super.singleCpuLightConcurrent();
    assertThat(threads.size(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(2)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), allOf(
                                                                                          greaterThanOrEqualTo(1l),
                                                                                          lessThanOrEqualTo(2l)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when all processor are CPU_LIGHT then they are all exectured in a single "
      + " cpu light thread.")
  public void multipleCpuLight() throws Exception {
    super.multipleCpuLight();
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, a BLOCKING message processor is scheduled on a IO thread.")
  public void singleBlocking() throws Exception {
    super.singleBlocking();
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, each BLOCKING message processor is scheduled on a IO thread. These may, or "
      + "may not, be the same thread.")
  public void multipleBlocking() throws Exception {
    super.multipleBlocking();
    assertThat(threads.size(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(3)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), allOf(
                                                                                   greaterThanOrEqualTo(1l),
                                                                                   lessThanOrEqualTo(3l)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, a CPU_INTENSIVE message processor is scheduled on a CPU intensive thread.")
  public void singleCpuIntensive() throws Exception {
    super.singleCpuIntensive();
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, each CPU_INTENSIVE message processor is scheduled on a CPU Intensive thread."
      + " These may, or may not, be the same thread.")
  public void multipleCpuIntensive() throws Exception {
    super.multipleCpuIntensive();
    assertThat(threads.size(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(3)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), allOf(
                                                                                              greaterThanOrEqualTo(1l),
                                                                                              lessThanOrEqualTo(3l)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when there is a mix of processor processing types, each processor is "
      + "scheduled on the correct scheduler.")
  public void mix() throws Exception {
    super.mix();
    assertThat(threads.size(), equalTo(3));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));

  }

  @Override
  @Description("With the ProactorProcessingStrategy, when there is a mix of processor processing types, each processor is "
      + "scheduled on the correct scheduler.")
  public void mix2() throws Exception {
    super.mix2();
    assertThat(threads.size(), allOf(greaterThanOrEqualTo(3), lessThanOrEqualTo(7)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(),
               allOf(greaterThanOrEqualTo(1l), lessThanOrEqualTo(2l)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(),
               allOf(greaterThanOrEqualTo(1l), lessThanOrEqualTo(2l)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), allOf(
                                                                                          greaterThanOrEqualTo(1l),
                                                                                          lessThanOrEqualTo(3l)));
  }

  @Override
  @Description("When the ProactorProcessingStrategy is configured and a transaction is active processing fails with an error")
  public void tx() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(hasMessage(equalTo(TRANSACTIONAL_ERROR_MESSAGE)));
    process(flow, testEvent());
  }

}
