/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE_MAP;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_PRINT_DETAILED_COMPOSITE_EXCEPTION_LOG_PROPERTY;
import static org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ScatterGatherStory.SCATTER_GATHER;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
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
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.processor.ContextPropagationChecker;

import java.io.StringBufferInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@Feature(ROUTERS)
@Story(SCATTER_GATHER)
public class ScatterGatherRouterTestCase extends AbstractMuleContextTestCase {

  private static final String KEY = "key";
  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";

  @Rule
  public SystemProperty detailedCompositeRoutingExceptionLog;

  @Rule
  public ExpectedException expectedException = none();

  private final ScatterGatherRouter router = new ScatterGatherRouter();
  private final ForkJoinStrategyFactory mockForkJoinStrategyFactory = mock(ForkJoinStrategyFactory.class);
  private final ConfigurationProperties configurationProperties = mock(ConfigurationProperties.class);


  public ScatterGatherRouterTestCase(boolean detailedCompositeRoutingExceptionLog) {
    this.detailedCompositeRoutingExceptionLog = new SystemProperty(MULE_PRINT_DETAILED_COMPOSITE_EXCEPTION_LOG_PROPERTY,
                                                                   Boolean.toString(detailedCompositeRoutingExceptionLog));
  }

  @Parameterized.Parameters(name = "Detailed log: {0}")
  public static List<Object[]> parameters() {
    return asList(
                  new Object[] {true},
                  new Object[] {false});
  }


  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    when(componentLocator.find(Location.builder().globalName(APPLE_FLOW).build())).thenReturn(of(mock(Flow.class)));

    Map<String, Object> registryObjects = new HashMap<>();
    registryObjects.put(REGISTRY_KEY, componentLocator);
    registryObjects.put("_configurationProperties", configurationProperties);
    return registryObjects;
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

    muleContext.getInjector().inject(router);
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

    muleContext.getInjector().inject(router);
    router.setRoutes(asList(route1, route2));
    router.setTarget(variableName);
    router.setTargetValue("#[message]");
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
                                                               any(Scheduler.class), any(ErrorType.class),
                                                               eq(Boolean.parseBoolean(detailedCompositeRoutingExceptionLog
                                                                   .getValue())));
  }


  @Test
  @Description("By default CollectMapForkJoinStrategyFactory is used which aggregates routes into a message with a Map<Message> payload.")
  public void defaultForkJoinStrategyFactory() {
    assertThat(router.getDefaultForkJoinStrategyFactory(), instanceOf(CollectMapForkJoinStrategyFactory.class));
    assertThat(router.getDefaultForkJoinStrategyFactory().getResultDataType(), equalTo(MULE_MESSAGE_MAP));
  }

  @Test
  @Description("Consumable payloads are not supported.")
  public void consumablePayload() throws Exception {
    MessageProcessorChain route1 = newChain(empty(), event -> event);
    MessageProcessorChain route2 = newChain(empty(), event -> event);

    muleContext.getInjector().inject(router);
    router.setRoutes(asList(route1, route2));
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    expectedException.expect(instanceOf(MuleRuntimeException.class));
    router.process(CoreEvent.builder(testEvent()).message(Message.of(new StringBufferInputStream(TEST_PAYLOAD))).build());
  }

  @Test
  @Issue("W-11932094")
  @Description("An unmodifiable list in the first Scatter Gather's route must be able to be merged with the variables of the other routes.")
  public void mergeVariablesWhenTheFirstRouteHasAnUnmodifiableList() throws Exception {
    List<String> unmodifiableList = Collections.singletonList(VALUE_1);
    MessageProcessorChain route1 = newChain(empty(), event -> addVariable(event, KEY, unmodifiableList));
    MessageProcessorChain route2 = newChain(empty(), event -> addVariable(event, KEY, VALUE_2));

    muleContext.getInjector().inject(router);
    router.setRoutes(asList(route1, route2));
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    CoreEvent process = router.process(CoreEvent.builder(testEvent()).message(Message.of(TEST_PAYLOAD)).build());
    List<String> list = (List) process.getVariables().get(KEY).getValue();
    assertThat(list, contains(VALUE_1, VALUE_2));
  }

  @Test
  @Issue("W-11932094")
  @Description("An unmodifiable list in the second Scatter Gather's route must be able to be merged with the variables of the other routes.")
  public void mergeVariablesWhenTheSecondRouteHasAnUnmodifiableList() throws Exception {
    List<String> unmodifiableList = Collections.singletonList(VALUE_2);
    MessageProcessorChain route1 = newChain(empty(), event -> addVariable(event, KEY, VALUE_1));
    MessageProcessorChain route2 = newChain(empty(), event -> addVariable(event, KEY, unmodifiableList));

    muleContext.getInjector().inject(router);
    router.setRoutes(asList(route1, route2));
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    CoreEvent process = router.process(CoreEvent.builder(testEvent()).message(Message.of(TEST_PAYLOAD)).build());
    List<String> list = (List) process.getVariables().get(KEY).getValue();
    assertThat(list, contains(VALUE_1, unmodifiableList));
  }

  private CoreEvent addVariable(CoreEvent event, String key, Object value) {
    return CoreEvent.builder(event).addVariable(key, value).build();
  }

  @Test
  @Description("Delay errors is always true for scatter-gather currently.")
  public void defaultDelayErrors() {
    assertThat(router.isDelayErrors(), equalTo(true));
  }

  @Test
  public void subscriberContextPropagation() throws MuleException {
    final ContextPropagationChecker contextPropagationCheckerA = new ContextPropagationChecker();
    final ContextPropagationChecker contextPropagationCheckerB = new ContextPropagationChecker();

    muleContext.getInjector().inject(router);
    router.setRoutes(asList(newChain(empty(), contextPropagationCheckerA),
                            newChain(empty(), contextPropagationCheckerB)));
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    assertContextPropagation(testEvent(), router, contextPropagationCheckerA);
    assertContextPropagation(testEvent(), router, contextPropagationCheckerB);
  }
}
