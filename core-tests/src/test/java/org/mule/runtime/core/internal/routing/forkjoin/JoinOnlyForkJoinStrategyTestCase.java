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

import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair;

import org.junit.Test;


public class JoinOnlyForkJoinStrategyTestCase extends AbstractForkJoinStrategyTestCase {

  @Override
  protected ForkJoinStrategy createStrategy(long timeout) {
    return new JoinOnlyForkJoinStrategy(timeout);
  }

  @Test
  public void joinOnly() throws Exception {

    InternalEvent original = testEvent();

    Processor processor1 = createEchoProcessorSpy();
    Processor processor2 = createEchoProcessorSpy();
    Processor processor3 = createEchoProcessorSpy();

    RoutingPair pair1 = of(testEvent(), processor1);
    RoutingPair pair2 = of(testEvent(), processor2);
    RoutingPair pair3 = of(testEvent(), processor3);

    InternalEvent result =
        from(strategy.forkJoin(original, fromIterable(asList(pair1, pair2, pair3)), processingStrategy, 3, true)).block();

    assertThat(result, is(original));
    verify(processor1, times(1)).process(any(InternalEvent.class));
    verify(processor2, times(1)).process(any(InternalEvent.class));
    verify(processor2, times(1)).process(any(InternalEvent.class));
  }

}
