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
import static org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair.of;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair;

import org.junit.Test;


public class JoinOnlyForkJoinStrategyTestCase extends AbstractForkJoinStrategyTestCase {

  @Override
  protected ForkJoinStrategy createStrategy(ProcessingStrategy processingStrategy, int concurrency, boolean delayErrors,
                                            long timeout) {
    return new JoinOnlyForkJoinStrategyFactory().createForkJoinStrategy(processingStrategy, concurrency, delayErrors, timeout,
                                                                        timeoutErrorType);
  }

  @Test
  public void joinOnly() throws Throwable {

    Event original = testEvent();

    Processor processor1 = createEchoProcessorSpy();
    Processor processor2 = createEchoProcessorSpy();
    Processor processor3 = createEchoProcessorSpy();

    RoutingPair pair1 = of(testEvent(), createChain(processor1));
    RoutingPair pair2 = of(testEvent(), createChain(processor2));
    RoutingPair pair3 = of(testEvent(), createChain(processor3));

    Event result = invokeStrategyBlocking(strategy, original, asList(pair1, pair2, pair3));

    assertThat(result, is(original));
    verify(processor1, times(1)).process(any(Event.class));
    verify(processor2, times(1)).process(any(Event.class));
    verify(processor2, times(1)).process(any(Event.class));
  }

}
