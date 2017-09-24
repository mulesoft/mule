/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.test.allure.AllureConstants.ForkJoinStrategiesFeature.ForkJoinStrategiesStory.JOIN_ONLY;

import org.junit.Test;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;

import io.qameta.allure.Description;
import io.qameta.allure.Story;

@Story(JOIN_ONLY)
public class JoinOnlyForkJoinStrategyTestCase extends AbstractForkJoinStrategyTestCase {

  @Override
  protected ForkJoinStrategy createStrategy(ProcessingStrategy processingStrategy, int concurrency, boolean delayErrors,
                                            long timeout) {
    return new JoinOnlyForkJoinStrategyFactory().createForkJoinStrategy(processingStrategy, concurrency, delayErrors, timeout,
                                                                        scheduler,
                                                                        timeoutErrorType);
  }

  @Test
  @Description("This strategy waits for all routes to return and then returns the original event.")
  public void joinOnly() throws Throwable {

    CoreEvent original = testEvent();

    Processor processor1 = createProcessorSpy(of(1));
    Processor processor2 = createProcessorSpy(of(2));
    Processor processor3 = createProcessorSpy(of(3));

    RoutingPair pair1 = createRoutingPair(processor1);
    RoutingPair pair2 = createRoutingPair(processor2);
    RoutingPair pair3 = createRoutingPair(processor3);


    CoreEvent result = invokeStrategyBlocking(strategy, original, asList(pair1, pair2, pair3));

    assertThat(result, is(original));
    verify(processor1, times(1)).process(any(CoreEvent.class));
    verify(processor2, times(1)).process(any(CoreEvent.class));
    verify(processor2, times(1)).process(any(CoreEvent.class));
  }

}
