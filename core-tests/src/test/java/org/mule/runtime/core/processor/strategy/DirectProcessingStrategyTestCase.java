/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.core.processor.strategy.DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.SYNCHRONOUS;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(PROCESSING_STRATEGIES)
@Stories(SYNCHRONOUS)
public class DirectProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public DirectProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return DIRECT_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void singleCpuLight() throws Exception {
    super.singleCpuLight();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void singleCpuLightConcurrent() throws Exception {
    super.internalSingleCpuLightConcurrent(false);
    assertSynchronous(2);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void multipleCpuLight() throws Exception {
    super.multipleCpuLight();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void singleBlocking() throws Exception {
    super.singleBlocking();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void multipleBlocking() throws Exception {
    super.multipleBlocking();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void singleCpuIntensive() throws Exception {
    super.singleCpuIntensive();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void multipleCpuIntensive() throws Exception {
    super.multipleCpuIntensive();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void mix() throws Exception {
    super.mix();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void mix2() throws Exception {
    super.mix2();
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void tx() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    process(flow, testEvent());

    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread but async processors will cause additional threads to be used.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
    assertAsyncCpuLight();
  }

  protected void assertAsyncCpuLight() {
    assertThat(threads, hasSize(2));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CUSTOM)).count(), equalTo(1l));
  }

  protected void assertSynchronous(int concurrency) {
    assertThat(threads, hasSize(concurrency));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

}
