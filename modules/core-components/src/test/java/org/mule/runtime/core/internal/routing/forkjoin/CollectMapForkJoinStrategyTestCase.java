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
import static org.mule.test.allure.AllureConstants.ForkJoinStrategiesFeature.ForkJoinStrategiesStory.COLLECT_MAP;

import java.util.Map;

import org.junit.Test;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;

import io.qameta.allure.Description;
import io.qameta.allure.Story;

@Story(COLLECT_MAP)
public class CollectMapForkJoinStrategyTestCase extends AbstractForkJoinStrategyTestCase {


  @Override
  protected ForkJoinStrategy createStrategy(ProcessingStrategy processingStrategy, int concurrency, boolean delayErrors,
                                            long timeout) {
    return new CollectMapForkJoinStrategyFactory().createForkJoinStrategy(processingStrategy, concurrency, delayErrors, timeout,
                                                                          scheduler,
                                                                          timeoutErrorType);
  }

  @Test
  @Description("This strategy waits for all routes to return and then collects results into a map where the key of each entry is the string representation of the index of the routing pair.")
  public void collectMap() throws Throwable {
    Message route1Result = of(1);
    Message route2Result = of(2);
    Message route3Result = of(3);

    RoutingPair pair1 = createRoutingPair(route1Result);
    RoutingPair pair2 = createRoutingPair(route2Result);
    RoutingPair pair3 = createRoutingPair(route3Result);

    CoreEvent result = invokeStrategyBlocking(strategy, testEvent(), asList(pair1, pair2, pair3));

    assertThat(result.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, Message> resultMap = (Map<String, Message>) result.getMessage().getPayload().getValue();
    assertThat(resultMap.entrySet(), hasSize(3));
    assertThat(resultMap.get("0"), is(route1Result));
    assertThat(resultMap.get("1"), is(route2Result));
    assertThat(resultMap.get("2"), is(route3Result));
  }

}
