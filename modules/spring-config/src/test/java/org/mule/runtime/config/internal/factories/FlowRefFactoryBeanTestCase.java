/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.chain.SubflowMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.routing.RoutePathNotFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockSettings;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationContext;

@SmallTest
public class FlowRefFactoryBeanTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final MockSettings INITIALIZABLE_MESSAGE_PROCESSOR =
      withSettings().extraInterfaces(Component.class, Processor.class, Initialisable.class, Disposable.class,
                                     Startable.class, Stoppable.class);
  private static final String STATIC_REFERENCED_FLOW = "staticReferencedFlow";
  private static final String DYNAMIC_REFERENCED_FLOW = "dynamicReferencedFlow";
  private static final String PARSED_DYNAMIC_REFERENCED_FLOW = "parsedDynamicReferencedFlow";
  private static final String DYNAMIC_NON_EXISTANT = "#['nonExistant']";

  private CoreEvent result;
  private final ProcessingStrategy callerFlowProcessingStrategy = mock(ProcessingStrategy.class);
  private final Flow callerFlow = mock(Flow.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final Flow targetFlow = mock(Flow.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final MessageProcessorChain targetSubFlow = mock(MessageProcessorChain.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final Processor targetSubFlowChild = (Processor) mock(Object.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final SubflowMessageProcessorChainBuilder targetSubFlowChainBuilder = spy(new SubflowMessageProcessorChainBuilder());
  private final ApplicationContext applicationContext = mock(ApplicationContext.class);
  private ExtendedExpressionManager expressionManager;
  private MuleContext mockMuleContext;

  @Inject
  private ConfigurationComponentLocator locator;

  public FlowRefFactoryBeanTestCase() throws MuleException {}

  @Before
  public void setup() throws MuleException {
    result = testEvent();
    mockMuleContext = mockContextWithServices();
    expressionManager = mockMuleContext.getExpressionManager();
    doReturn(true).when(expressionManager).isExpression(anyString());
    when(targetFlow.apply(any(Publisher.class))).thenReturn(just(result));
    when(targetFlow.referenced()).thenReturn(targetFlow);

    List<Processor> targetSubFlowProcessors = singletonList(targetSubFlowChild);
    when(targetSubFlow.getMessageProcessors()).thenReturn(targetSubFlowProcessors);
    targetSubFlowChainBuilder.chain(targetSubFlowProcessors);
    when(targetSubFlowChild.apply(any(Publisher.class))).thenReturn(just(result));
    when(targetSubFlow.apply(any(Publisher.class))).thenReturn(just(result));

    mockMuleContext.getInjector().inject(this);

    when(locator.find(any(Location.class))).thenReturn(of(mock(Flow.class)));
    when(locator.find(Location.builder().globalName("flow").build())).thenReturn(of(callerFlow));
    when(callerFlow.getProcessingStrategy()).thenReturn(callerFlowProcessingStrategy);

    when(callerFlowProcessingStrategy.onProcessor(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
  }

  @Test
  public void staticFlowRefFlow() throws Exception {
    // Flow is wrapped to prevent lifecycle propagation
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetFlow, null);

    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetFlow);
    verifyLifecycle(targetFlow, 0);
  }

  @Test
  public void dynamicFlowRefFlow() throws Exception {
    // Inner MessageProcessor is used to resolve MP in runtime
    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetFlow, null);

    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetFlow);
    verifyLifecycle(targetFlow, 0);
  }

  @Test
  public void staticFlowRefSubFlow() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow, targetSubFlowChainBuilder);

    // Processor is wrapped by factory bean implementation
    assertThat(targetSubFlow, not(equalTo(getFlowRefProcessor(flowRefFactoryBean))));
    assertThat(targetSubFlow, not(equalTo(getFlowRefProcessor(flowRefFactoryBean))));

    verifyProcess(flowRefFactoryBean, targetSubFlowChild);
    verify(targetSubFlowChainBuilder).setProcessingStrategy(argThat(ps -> {
      ReactiveProcessor pipeline = mock(ReactiveProcessor.class);
      ReactiveProcessor processor = mock(ReactiveProcessor.class);

      ps.onProcessor(processor);
      verify(callerFlowProcessingStrategy).onProcessor(processor);
      assertThat(ps.onPipeline(pipeline), sameInstance(pipeline));
      return true;
    }));
  }

  @Test
  public void dynamicFlowRefSubFlow() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlow, targetSubFlowChainBuilder);

    // Inner MessageProcessor is used to resolve MP in runtime
    assertNotSame(targetSubFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetSubFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetSubFlowChild);
    verify(targetSubFlowChainBuilder).setProcessingStrategy(argThat(ps -> {
      ReactiveProcessor pipeline = mock(ReactiveProcessor.class);
      ReactiveProcessor processor = mock(ReactiveProcessor.class);

      ps.onProcessor(processor);
      verify(callerFlowProcessingStrategy).onProcessor(processor);
      assertThat(ps.onPipeline(pipeline), sameInstance(pipeline));
      return true;
    }));
  }

  @Test
  public void dynamicFlowRefSubContextAware() throws Exception {
    CoreEvent event = testEvent();
    MuleContextAware targetMuleContextAware = mock(MuleContextAware.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(((Processor) targetMuleContextAware).apply(any(Publisher.class))).thenReturn(just(result));

    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean((Processor) targetMuleContextAware, null);
    assertSame(result.getMessage(), getFlowRefProcessor(flowRefFactoryBean).process(event).getMessage());

    verify(targetMuleContextAware).setMuleContext(mockMuleContext);
  }

  @Test
  public void dynamicFlowRefSubFlowMessageProcessorChain() throws Exception {
    CoreEvent event = testEvent();

    Processor targetSubFlowConstructAware = (Processor) mock(Object.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetSubFlowConstructAware.apply(any(Publisher.class))).thenReturn(just(result));
    Processor targetMuleContextAwareAware =
        (Processor) mock(MuleContextAware.class, INITIALIZABLE_MESSAGE_PROCESSOR);

    when(targetMuleContextAwareAware.apply(any(Publisher.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

    MessageProcessorChain targetSubFlowChain = mock(MessageProcessorChain.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetSubFlowChain.apply(any(Publisher.class))).thenReturn(just(result));
    List<Processor> targetSubFlowProcessors = asList(targetSubFlowConstructAware, targetMuleContextAwareAware);
    when(targetSubFlowChain.getMessageProcessors()).thenReturn(targetSubFlowProcessors);
    SubflowMessageProcessorChainBuilder chainBuilder = new SubflowMessageProcessorChainBuilder();
    chainBuilder.chain(targetSubFlowProcessors);

    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlowChain, chainBuilder);
    final Processor flowRefProcessor = getFlowRefProcessor(flowRefFactoryBean);
    just(event).transform(flowRefProcessor).block();

    verify((MuleContextAware) targetMuleContextAwareAware, atLeastOnce()).setMuleContext(mockMuleContext);
  }

  private Processor getFlowRefProcessor(FlowRefFactoryBean factoryBean) throws Exception {
    Processor processor = factoryBean.getObject();
    setMuleContextIfNeeded(processor, mockMuleContext);
    return processor;
  }

  @Test
  public void dynamicFlowRefDoesNotExist() throws Exception {
    doReturn(true).when(expressionManager).isExpression(anyString());
    doReturn(new TypedValue<>("other", STRING)).when(expressionManager).evaluate(eq(DYNAMIC_NON_EXISTANT), any(CoreEvent.class),
                                                                                 any(ComponentLocation.class));

    expectedException.expect(instanceOf(RoutePathNotFoundException.class));
    getFlowRefProcessor(createFlowRefFactoryBean(DYNAMIC_NON_EXISTANT)).process(testEvent());
  }

  private FlowRefFactoryBean createFlowRefFactoryBean(String name) throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
    flowRefFactoryBean.setName(name);
    flowRefFactoryBean.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    flowRefFactoryBean.setApplicationContext(applicationContext);
    mockMuleContext.getInjector().inject(flowRefFactoryBean);
    return flowRefFactoryBean;
  }

  private FlowRefFactoryBean createStaticFlowRefFactoryBean(Processor target, Object targetBuilder)
      throws Exception {
    doReturn(false).when(expressionManager).isExpression(anyString());
    if (targetBuilder != null) {
      when(applicationContext.getBean(eq(STATIC_REFERENCED_FLOW))).thenReturn(targetBuilder);
    } else {
      when(applicationContext.getBean(eq(STATIC_REFERENCED_FLOW))).thenReturn(target);
    }

    if (target instanceof MessageProcessorChain) {
      Processor processor = ((MessageProcessorChain) target).getMessageProcessors().get(0);
      when(processor.apply(any())).thenAnswer(successAnswer());
    } else {
      when(target.apply(any())).thenAnswer(successAnswer());
    }

    return createFlowRefFactoryBean(STATIC_REFERENCED_FLOW);
  }

  private FlowRefFactoryBean createDynamicFlowRefFactoryBean(Processor target, Object targetBuilder)
      throws Exception {
    doReturn(true).when(expressionManager).isExpression(anyString());
    doReturn(new TypedValue<>(PARSED_DYNAMIC_REFERENCED_FLOW, STRING)).when(expressionManager)
        .evaluate(eq(DYNAMIC_REFERENCED_FLOW), any(CoreEvent.class),
                  any(ComponentLocation.class));
    if (targetBuilder != null) {
      when(applicationContext.getBean(eq(PARSED_DYNAMIC_REFERENCED_FLOW))).thenReturn(targetBuilder);
    } else {
      when(applicationContext.getBean(eq(PARSED_DYNAMIC_REFERENCED_FLOW))).thenReturn(target);
    }

    if (target instanceof MessageProcessorChain) {
      Processor processor = ((MessageProcessorChain) target).getMessageProcessors().get(0);
      when(processor.apply(any())).thenAnswer(successAnswer());
    } else {
      when(target.apply(any())).thenAnswer(successAnswer());
    }

    return createFlowRefFactoryBean(DYNAMIC_REFERENCED_FLOW);
  }

  private Answer<?> successAnswer() {
    return invocation -> {
      return from(invocation.getArgument(0))
          .cast(CoreEvent.class)
          .doOnNext(event -> ((BaseEventContext) event.getContext())
              .success(CoreEvent.builder(event).message(result.getMessage()).variables(result.getVariables()).build()))
          .map(event -> CoreEvent.builder(event).message(result.getMessage()).variables(result.getVariables()).build());
    };
  }

  private void verifyProcess(FlowRefFactoryBean flowRefFactoryBean, Processor target)
      throws Exception {
    Processor flowRefProcessor = getFlowRefProcessor(flowRefFactoryBean);
    initialiseIfNeeded(flowRefProcessor);
    startIfNeeded(flowRefProcessor);

    assertSame(result.getMessage(), just(newEvent()).cast(CoreEvent.class).transform(flowRefProcessor).block().getMessage());
    assertSame(result.getMessage(), just(newEvent()).cast(CoreEvent.class).transform(flowRefProcessor).block().getMessage());

    verify(applicationContext).getBean(anyString());

    verify(target, times(2)).apply(any(Publisher.class));

    stopIfNeeded(flowRefProcessor);
    disposeIfNeeded(flowRefProcessor, null);
  }

  private void verifyLifecycle(Processor target, int lifecycleRounds)
      throws Exception {
    verify((Initialisable) target, times(lifecycleRounds)).initialise();

    verify(targetSubFlow, times(lifecycleRounds)).initialise();
    verify(targetSubFlow, times(lifecycleRounds)).start();
    verify(targetSubFlow, times(lifecycleRounds)).stop();
    verify(targetSubFlow, times(lifecycleRounds)).dispose();
  }

  @Test
  public void referencedSubFlowIsStartedWhenCallerFlowIsStartedAfterStop() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow, null);
    Processor flowRefProcessor = flowRefFactoryBean.doGetObject();

    flowRefProcessor.process(testEvent());

    verify(targetSubFlow, times(1)).initialise();
    verify(targetSubFlow, times(1)).start();
    verify(targetSubFlow, times(0)).stop();
    verify(targetSubFlow, times(0)).dispose();

    stopIfNeeded(flowRefProcessor);

    verify(targetSubFlow, times(1)).initialise();
    verify(targetSubFlow, times(1)).start();
    verify(targetSubFlow, times(1)).stop();
    verify(targetSubFlow, times(0)).dispose();

    startIfNeeded(flowRefProcessor);

    verify(targetSubFlow, times(1)).initialise();
    verify(targetSubFlow, times(2)).start();
    verify(targetSubFlow, times(1)).stop();
    verify(targetSubFlow, times(0)).dispose();
  }

}
