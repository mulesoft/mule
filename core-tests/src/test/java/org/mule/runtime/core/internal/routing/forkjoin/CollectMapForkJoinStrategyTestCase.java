/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair.of;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair;

import java.util.Map;

import org.junit.Test;


public class CollectMapForkJoinStrategyTestCase extends AbstractForkJoinStrategyTestCase {


  @Override
  protected ForkJoinStrategy createStrategy(long timeout) {
    return new CollectMapForkJoinStrategy(timeout);
  }

  @Test
  public void collectMap() throws Exception {

    Event original = testEvent();
    Message route1Result = of("1");
    Message route2Result = of("2");
    Message route3Result = of("3");

    RoutingPair pair1 = of(testEvent(), event -> builder(event).message(route1Result).build());
    RoutingPair pair2 = of(testEvent(), event -> builder(event).message(route2Result).build());
    RoutingPair pair3 = of(testEvent(), event -> builder(event).message(route3Result).build());

    Event result =
        from(strategy.forkJoin(original, fromIterable(asList(pair1, pair2, pair3)),
                               processingStrategy, 1, true)).block();

    assertThat(result.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, Message> resultMap = (Map<String, Message>) result.getMessage().getPayload().getValue();
    assertThat(resultMap.entrySet(), hasSize(3));
    assertThat(resultMap.get("0"), is(route1Result));
    assertThat(resultMap.get("1"), is(route2Result));
    assertThat(resultMap.get("2"), is(route3Result));
  }

}
