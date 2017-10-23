/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.routing.RoutePathNotFoundException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockSettings;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

import reactor.core.publisher.Mono;

@SmallTest
public class FlowRefFactoryBeanTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final MockSettings INITIALIZABLE_MESSAGE_PROCESSOR =
      withSettings().extraInterfaces(Component.class, Processor.class, Initialisable.class, Disposable.class,
                                     Startable.class, Stoppable.class);
  private static final String STATIC_REFERENCED_FLOW = "staticReferencedFlow";
  private static final String DYNAMIC_REFERENCED_FLOW = "dynamicReferencedFlow";
  private static final String PARSED_DYNAMIC_REFERENCED_FLOW = "parsedDynamicReferencedFlow";
  private static final String DYNAMIC_NON_EXISTANT = "#['nonExistant']";
  private static final String NON_EXISTANT = "nonExistant";

  private CoreEvent result;
  private Flow targetFlow = mock(Flow.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private MessageProcessorChain targetSubFlow = mock(MessageProcessorChain.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private ApplicationContext applicationContext = mock(ApplicationContext.class);
  private ExtendedExpressionManager expressionManager;
  private MuleContext mockMuleContext;

  public FlowRefFactoryBeanTestCase() throws MuleException {}

  @Before
  public void setup() throws MuleException {
    result = testEvent();
    expressionManager = spy(muleContext.getExpressionManager());
    mockMuleContext = spy(muleContext);
    doReturn(expressionManager).when(mockMuleContext).getExpressionManager();
    doReturn(true).when(expressionManager).isExpression(anyString());
    when(targetFlow.apply(any(Publisher.class))).thenReturn(just(result));
    when(targetSubFlow.apply(any(Publisher.class))).thenReturn(just(result));
  }

  @Test
  public void staticFlowRefFlow() throws Exception {
    // Flow is wrapped to prevent lifecycle propagation
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetFlow);

    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetFlow, 0);
  }

  @Test
  public void dynamicFlowRefFlow() throws Exception {
    // Inner MessageProcessor is used to resolve MP in runtime
    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetFlow);

    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetFlow, 0);
  }

  @Test
  public void staticFlowRefSubFlow() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow);

    // Processor is wrapped by factory bean implementation
    assertThat(targetSubFlow, not(equalTo(getFlowRefProcessor(flowRefFactoryBean))));
    assertThat(targetSubFlow, not(equalTo(getFlowRefProcessor(flowRefFactoryBean))));

    verifyProcess(flowRefFactoryBean, targetSubFlow, 1);
  }

  @Test
  public void dynamicFlowRefSubFlow() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlow);

    // Inner MessageProcessor is used to resolve MP in runtime
    assertNotSame(targetSubFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetSubFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetSubFlow, 1);
  }

  @Test
  public void dynamicFlowRefSubContextAware() throws Exception {
    CoreEvent event = testEvent();
    MuleContextAware targetMuleContextAware = mock(MuleContextAware.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(((Processor) targetMuleContextAware).apply(any(Publisher.class))).thenReturn(just(result));

    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean((Processor) targetMuleContextAware);
    assertSame(result.getMessage(), getFlowRefProcessor(flowRefFactoryBean).process(event).getMessage());

    verify(targetMuleContextAware).setMuleContext(mockMuleContext);
  }

  @Test
  public void dynamicFlowRefSubFlowMessageProcessorChain() throws Exception {
    CoreEvent event = testEvent();

    Processor targetSubFlowConstructAware = (Processor) mock(Object.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetSubFlowConstructAware.process(any(CoreEvent.class))).thenReturn(result);
    Processor targetMuleContextAwareAware =
        (Processor) mock(MuleContextAware.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetMuleContextAwareAware.process(any(CoreEvent.class))).thenReturn(result);

    MessageProcessorChain targetSubFlowChain = mock(MessageProcessorChain.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetSubFlowChain.apply(any(Publisher.class))).thenReturn(just(result));
    when(targetSubFlowChain.getMessageProcessors())
        .thenReturn(Arrays.asList(targetSubFlowConstructAware, targetMuleContextAwareAware));

    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlowChain);
    final Processor flowRefProcessor = getFlowRefProcessor(flowRefFactoryBean);
    just(event).transform(flowRefProcessor).block();

    verify((MuleContextAware) targetMuleContextAwareAware).setMuleContext(mockMuleContext);
  }

  private Processor getFlowRefProcessor(FlowRefFactoryBean factoryBean) throws Exception {
    Processor processor = factoryBean.getObject();
    setMuleContextIfNeeded(processor, mockMuleContext);
    return processor;
  }

  @Test
  public void dynamicFlowRefDoesNotExist() throws Exception {
    doReturn(true).when(expressionManager).isExpression(anyString());
    doReturn("other").when(expressionManager).parse(eq(DYNAMIC_NON_EXISTANT), any(CoreEvent.class),
                                                    any(ComponentLocation.class));

    expectedException.expect(instanceOf(RoutePathNotFoundException.class));
    getFlowRefProcessor(createFlowRefFactoryBean(DYNAMIC_NON_EXISTANT)).process(testEvent());
  }

  private FlowRefFactoryBean createFlowRefFactoryBean(String name) throws InitialisationException {
    FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
    flowRefFactoryBean.setName(name);
    flowRefFactoryBean.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    flowRefFactoryBean.setApplicationContext(applicationContext);
    flowRefFactoryBean.setMuleContext(mockMuleContext);
    return flowRefFactoryBean;
  }

  private FlowRefFactoryBean createStaticFlowRefFactoryBean(Processor target) throws InitialisationException {
    doReturn(false).when(expressionManager).isExpression(anyString());
    when(applicationContext.getBean(eq(STATIC_REFERENCED_FLOW))).thenReturn(target);
    when(target.apply(any())).thenAnswer(invocation -> {
      Mono<CoreEvent> mono = from(invocation.getArgumentAt(0, Publisher.class));
      return mono.doOnNext(event -> ((BaseEventContext) event.getContext()).success(result)).map(event -> result);
    });
    return createFlowRefFactoryBean(STATIC_REFERENCED_FLOW);
  }

  private FlowRefFactoryBean createDynamicFlowRefFactoryBean(Processor target) throws InitialisationException {
    doReturn(true).when(expressionManager).isExpression(anyString());
    doReturn(PARSED_DYNAMIC_REFERENCED_FLOW).when(expressionManager).parse(eq(DYNAMIC_REFERENCED_FLOW), any(CoreEvent.class),
                                                                           any(ComponentLocation.class));
    when(applicationContext.getBean(eq(PARSED_DYNAMIC_REFERENCED_FLOW))).thenReturn(target);
    when(target.apply(any())).thenAnswer(invocation -> {
      Mono<CoreEvent> mono = from(invocation.getArgumentAt(0, Publisher.class));
      return mono.doOnNext(event -> ((BaseEventContext) event.getContext()).success(result)).map(event -> result);
    });
    return createFlowRefFactoryBean(DYNAMIC_REFERENCED_FLOW);
  }

  private void verifyProcess(FlowRefFactoryBean flowRefFactoryBean, Processor target, int lifecycleRounds)
      throws Exception {
    Processor flowRefProcessor = getFlowRefProcessor(flowRefFactoryBean);
    initialiseIfNeeded(flowRefProcessor);
    startIfNeeded(flowRefProcessor);

    assertSame(result.getMessage(), just(newEvent()).cast(CoreEvent.class).transform(flowRefProcessor).block().getMessage());
    assertSame(result.getMessage(), just(newEvent()).cast(CoreEvent.class).transform(flowRefProcessor).block().getMessage());

    verify(applicationContext).getBean(anyString());

    verify(target, times(2)).apply(any(Publisher.class));
    verify((Initialisable) target, times(lifecycleRounds)).initialise();

    stopIfNeeded(flowRefProcessor);
    disposeIfNeeded(flowRefProcessor, null);
    verify(targetSubFlow, times(lifecycleRounds)).initialise();
    verify(targetSubFlow, times(lifecycleRounds)).start();
    verify(targetSubFlow, times(lifecycleRounds)).stop();
    verify(targetSubFlow, times(lifecycleRounds)).dispose();
  }

}
