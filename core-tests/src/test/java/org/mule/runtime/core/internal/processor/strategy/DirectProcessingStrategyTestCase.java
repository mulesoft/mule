/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.DIRECT;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PROCESSING_STRATEGIES)
@Story(DIRECT)
public class DirectProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public DirectProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new DirectProcessingStrategyFactory().create(muleContext, schedulersNamePrefix);
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
    super.internalConcurrent(flowBuilder.get(), false, CPU_LITE, 1);
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
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    processFlow(testEvent());

    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the DirectProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread but async processors will cause additional threads to be used. Flow processing "
      + "continues using async processor thread.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
    assertAsyncCpuLight();
  }

  @Override
  @Description("When using DirectProcessingStrategy continued processing is carried out using async processor thread which can "
      + "cause processing to block if there are concurrent requests and the number of custom async processor threads are reduced")
  public void asyncCpuLightConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), true, CPU_LITE, 1, asyncProcessor);
    assertThat(threads.size(), between(2, 3));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads.stream().filter(name -> name.startsWith(CUSTOM)).count(), equalTo(1l));
  }

  protected void assertAsyncCpuLight() {
    assertThat(threads, hasSize(2));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads.stream().filter(name -> name.startsWith(CUSTOM)).count(), equalTo(1l));
  }

  protected void assertSynchronous(int concurrency) {
    assertThat(threads, hasSize(concurrency));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

}
