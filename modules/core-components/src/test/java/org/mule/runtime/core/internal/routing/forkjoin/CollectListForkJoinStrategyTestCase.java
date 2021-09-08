/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.test.allure.AllureConstants.ForkJoinStrategiesFeature.ForkJoinStrategiesStory.COLLECT_LIST;

import java.util.List;

import org.junit.Test;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;

import io.qameta.allure.Description;
import io.qameta.allure.Story;

@Story(COLLECT_LIST)
public class CollectListForkJoinStrategyTestCase extends AbstractForkJoinStrategyTestCase {

  @Override
  protected ForkJoinStrategy createStrategy(ProcessingStrategy processingStrategy, int concurrency, boolean delayErrors,
                                            long timeout) {
    return new CollectListForkJoinStrategyFactory().createForkJoinStrategy(processingStrategy, concurrency, delayErrors, timeout,
                                                                           scheduler,
                                                                           timeoutErrorType);
  }

  @Test
  @Description("This strategy waits for all routes to return and then collects results into a list.")
  public void collectList() throws Throwable {

    CoreEvent original = testEvent();
    Message route1Result = of(1);
    Message route2Result = of(2);
    Message route3Result = of(3);

    RoutingPair pair1 = createRoutingPair(route1Result);
    RoutingPair pair2 = createRoutingPair(route2Result);
    RoutingPair pair3 = createRoutingPair(route3Result);

    CoreEvent result = invokeStrategyBlocking(strategy, original, asList(pair1, pair2, pair3));

    assertThat(result.getMessage().getPayload().getValue(), instanceOf(List.class));
    List<Message> resultList = (List<Message>) result.getMessage().getPayload().getValue();
    assertThat(resultList, hasSize(3));
    assertThat(resultList, hasItems(route1Result, route2Result, route3Result));
  }

  @Test
  @Description("Checks that variables are not merged if set as it")
  public void flowVarsNotMerged() throws Throwable {
    strategy = new CollectListForkJoinStrategyFactory(false).createForkJoinStrategy(processingStrategy, 1, true, 50, scheduler,
                                                                                    timeoutErrorType);
    final String beforeVarName = "before";
    final String beforeVarValue = "beforeValue";
    final String beforeVar2Name = "before2";
    final String beforeVar2Value = "before2Value";
    final String beforeVar2NewValue = "before2NewValue";
    final String fooVarName = "foo";
    final String fooVarValue = "fooValue1";
    final String fooVar2Name = "foo2";
    final String fooVar2Value1 = "foo2Value1";
    final String fooVar2Value2 = "foo2Value2";

    CoreEvent original = builder(this.<CoreEvent>newEvent())
        .addVariable(beforeVarName, beforeVarValue)
        .addVariable(beforeVar2Name, beforeVar2Value)
        .build();

    RoutingPair pair1 = RoutingPair.of(original, createChain(event -> builder(event)
        .addVariable(beforeVar2Name, beforeVar2NewValue)
        .addVariable(fooVarName, fooVarValue)
        .addVariable(fooVar2Name, fooVar2Value1)
        .build()));
    RoutingPair pair2 = RoutingPair.of(original, createChain(event -> builder(event)
        .addVariable(fooVar2Name, fooVar2Value2)
        .build()));

    CoreEvent result = invokeStrategyBlocking(strategy, original, asList(pair1, pair2));

    assertThat(result.getVariables().keySet(), hasSize(2));

    assertThat(result.getVariables().keySet(), hasItems(beforeVarName, beforeVar2Name));
    assertThat(result.getVariables().keySet(), not(hasItems(fooVarName, fooVarName, fooVar2Name)));

    assertThat(result.getVariables().get(beforeVarName).getValue(), equalTo(beforeVarValue));
    assertThat(result.getVariables().get(beforeVar2Name).getValue(), equalTo(beforeVar2Value));
  }

}
