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
import static org.mule.runtime.core.processor.strategy.AbstractSchedulingProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.strategy.ReactorProcessingStrategyFactory.ReactorProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Processing Strategies")
@Stories("MultiReactor Processing Strategy")
public class MultiReactorProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public MultiReactorProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorProcessingStrategy(() -> cpuLight, scheduler -> {
    }, muleContext);
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void singleCpuLight() throws Exception {
    super.singleCpuLight();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("When MultiReactorProcessingStrategy is configured, two concurrent requests may be processed by two different "
      + " cpu light threads.  This is why this strategy is called 'MultiReactor' and not 'Reactor`.  MULE-11132 is needed for "
      + "true reactor behaviour.")
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
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void multipleCpuLight() throws Exception {
    super.multipleCpuLight();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void singleBlocking() throws Exception {
    super.singleBlocking();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void multipleBlocking() throws Exception {
    super.multipleBlocking();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void singleCpuIntensive() throws Exception {
    super.singleCpuIntensive();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void multipleCpuIntensive() throws Exception {
    super.multipleCpuIntensive();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void mix() throws Exception {
    super.mix();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the MultiReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void mix2() throws Exception {
    super.mix2();
    assertEverythingOnEventLoop();
  }

  private void assertEverythingOnEventLoop() {
    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("When the MultiReactorProcessingStrategy is configured and a transaction is active processing fails with an error")
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
