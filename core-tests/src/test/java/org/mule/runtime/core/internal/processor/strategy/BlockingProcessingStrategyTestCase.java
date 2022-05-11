/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.BLOCKING;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.reactivestreams.Publisher;

@Feature(PROCESSING_STRATEGIES)
@Story(BLOCKING)
public class BlockingProcessingStrategyTestCase extends DirectProcessingStrategyTestCase {

  private static ComponentLocation componentLocation = mock(ComponentLocation.class);

  public BlockingProcessingStrategyTestCase(Mode mode, boolean profiling) {
    // The blocking processing strategy does not implement profiling yet
    super(mode, false);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return BLOCKING_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  @Description("Regardless of processor type, when the BlockingProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread and the pipeline will block caller thread until any async processors complete "
      + "before continuing in the caller thread.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
  }

  @Override
  protected void assertAsyncCpuLight() {
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the BlockingProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread and the pipeline will block caller thread until any async processors complete "
      + "before continuing in the caller thread.")
  public void asyncCpuLightConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), false, CPU_LITE, 1, asyncProcessor);
    assertSynchronous(2);
  }

  @Test
  @Issue("MULE-19426")
  @Description("In order to improve performance, we don't need to add a Mono#block on a blocking processor, because it "
      + "will run synchronously")
  public void blockingProcessorDoesNotNeedToBeDecorated() {
    assertThat(ps.onProcessor(blockingProcessor), is(sameInstance(blockingProcessor)));
  }

  @Test
  @Issue("MULE-19426")
  @Description("Processors with a completion callback should be decorated with a Mono#block to ensure they run "
      + "synchronously")
  public void processorsWithCompletionCallbackNeedToBeDecorated() {
    ReactiveProcessor processor = new NonBlockingWithProcessingTypeBlockingProcessor(componentLocation);
    assertThat(ps.onProcessor(processor), is(not(sameInstance(processor))));
  }

  private static class NonBlockingWithProcessingTypeBlockingProcessor implements ComponentInnerProcessor {

    private final ComponentLocation componentLocation;

    public NonBlockingWithProcessingTypeBlockingProcessor(ComponentLocation componentLocation) {
      this.componentLocation = componentLocation;
    }

    @Override
    public boolean isBlocking() {
      // Return false to indicate that this processor has a completion callback.
      return false;
    }

    @Override
    public ComponentLocation resolveLocation() {
      return componentLocation;
    }

    @Override
    public ProcessingType getProcessingType() {
      return ProcessingType.BLOCKING;
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> coreEventPublisher) {
      return coreEventPublisher;
    }
  }

}
