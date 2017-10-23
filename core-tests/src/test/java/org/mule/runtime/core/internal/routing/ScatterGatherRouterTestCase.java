/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE_MAP;
import static org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ScatterGatherStory.SCATTER_GATHER;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;
import org.mule.runtime.core.internal.routing.forkjoin.CollectMapForkJoinStrategyFactory;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.StringBufferInputStream;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(SCATTER_GATHER)
public class ScatterGatherRouterTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private ScatterGatherRouter router = new ScatterGatherRouter();
  private ForkJoinStrategyFactory mockForkJoinStrategyFactory = mock(ForkJoinStrategyFactory.class);

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    when(componentLocator.find(Location.builder().globalName(APPLE_FLOW).build())).thenReturn(of(mock(Flow.class)));
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @After
  public void tearDown() throws Exception {
    router.dispose();
  }

  @Test
  @Description("RoutingPairs are created for each route configured. Each RoutingPair has the same input event.")
  public void routingPairs() throws Exception {
    CoreEvent event = mock(CoreEvent.class);
    MessageProcessorChain route1 = mock(MessageProcessorChain.class);
    MessageProcessorChain route2 = mock(MessageProcessorChain.class);
    MessageProcessorChain route3 = mock(MessageProcessorChain.class);

    router.setRoutes(asList(route1, route2, route3));

    List<RoutingPair> routingPairs = from(router.getRoutingPairs(event)).collectList().block();
    assertThat(routingPairs, hasSize(3));
    assertThat(routingPairs.get(0), equalTo(of(event, route1)));
    assertThat(routingPairs.get(1), equalTo(of(event, route2)));
    assertThat(routingPairs.get(2), equalTo(of(event, route3)));
  }

  @Test
  @Description("By default the router result populates the outgoing message payload.")
  public void defaultTarget() throws Exception {
    CoreEvent original = testEvent();
    MessageProcessorChain route1 = newChain(empty(), event -> event);
    MessageProcessorChain route2 = newChain(empty(), event -> event);

    router.setRoutes(asList(route1, route2));
    muleContext.getInjector().inject(router);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    Event result = router.process(original);

    assertThat(result.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, Message> resultMap = (Map) result.getMessage().getPayload().getValue();
    assertThat(resultMap.values(), hasSize(2));
  }

  @Test
  @Description("When a custom target is configured the router result is set in a variable and the input event is output.")
  public void customTargetMessage() throws Exception {
    final String variableName = "foo";

    CoreEvent original = testEvent();
    MessageProcessorChain route1 = newChain(empty(), event -> event);
    MessageProcessorChain route2 = newChain(empty(), event -> event);

    router.setRoutes(asList(route1, route2));
    router.setTarget(variableName);
    router.setTargetValue("#[message]");
    muleContext.getInjector().inject(router);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    Event result = router.process(original);

    assertThat(result.getMessage(), equalTo(original.getMessage()));
    assertThat(((Message) result.getVariables().get(variableName).getValue()).getPayload().getValue(), instanceOf(Map.class));
    Map<String, Message> resultMap = (Map) ((Message) result.getVariables().get(variableName).getValue()).getPayload().getValue();
    assertThat(resultMap.values(), hasSize(2));
  }

  @Test
  @Description("When a custom target is configured the router result is set in a variable and the input event is output.")
  public void customTargetDefaultPayload() throws Exception {
    final String variableName = "foo";

    CoreEvent original = testEvent();
    MessageProcessorChain route1 = newChain(empty(), event -> event);
    MessageProcessorChain route2 = newChain(empty(), event -> event);

    router.setRoutes(asList(route1, route2));
    router.setTarget(variableName);
    muleContext.getInjector().inject(router);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    Event result = router.process(original);

    assertThat(result.getMessage(), equalTo(original.getMessage()));
    final TypedValue<?> typedValue = result.getVariables().get(variableName);
    assertThat(typedValue.getValue(), instanceOf(Map.class));
    assertThat(Map.class.isAssignableFrom(typedValue.getDataType().getType()), is(true));
    Map<String, Message> resultMap = (Map<String, Message>) typedValue.getValue();
    assertThat(resultMap.values(), hasSize(2));
  }

  @Test
  @Description("The router uses a fork-join strategy with concurrency and timeout configured via the router and delayErrors true.")
  public void forkJoinStrategyConfiguration() throws Exception {
    final int routes = 21;
    final int concurrency = 3;
    final long timeout = 123;

    router.setMaxConcurrency(concurrency);
    router.setTimeout(timeout);
    router.setRoutes(range(0, routes).mapToObj(i -> mock(MessageProcessorChain.class)).collect(toList()));
    router.setForkJoinStrategyFactory(mockForkJoinStrategyFactory);

    muleContext.getInjector().inject(router);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    verify(mockForkJoinStrategyFactory).createForkJoinStrategy(any(ProcessingStrategy.class), eq(concurrency), eq(true),
                                                               eq(timeout),
                                                               any(Scheduler.class), any(ErrorType.class));
  }


  @Test
  @Description("By default CollectMapForkJoinStrategyFactory is used which aggregates routes into a message with a Map<Message> payload.")
  public void defaultForkJoinStrategyFactory() throws Exception {
    assertThat(router.getDefaultForkJoinStrategyFactory(), instanceOf(CollectMapForkJoinStrategyFactory.class));
    assertThat(router.getDefaultForkJoinStrategyFactory().getResultDataType(), equalTo(MULE_MESSAGE_MAP));
  }

  @Test
  @Description("The router must be configured with at least two routes.")
  public void minimumTwoRoutes() throws Exception {
    expectedException.expect(instanceOf(IllegalArgumentException.class));
    router.setRoutes(singletonList(mock(MessageProcessorChain.class)));
  }

  @Test
  @Description("Consumable payloads are not supported.")
  public void consumablePayload() throws Exception {
    MessageProcessorChain route1 = newChain(empty(), event -> event);
    MessageProcessorChain route2 = newChain(empty(), event -> event);

    router.setRoutes(asList(route1, route2));
    muleContext.getInjector().inject(router);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    expectedException.expect(instanceOf(MuleRuntimeException.class));
    router.process(CoreEvent.builder(testEvent()).message(Message.of(new StringBufferInputStream(TEST_PAYLOAD))).build());
  }

  @Test
  @Description("Delay errors is always true for scatter-gather currently.")
  public void defaultDelayErrors() throws Exception {
    assertThat(router.isDelayErrors(), equalTo(true));
  }

}
