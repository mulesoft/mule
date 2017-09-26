/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.transformer.simple.StringAppendTransformer;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.core.lifecycle.LifecycleTrackerProcessor;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@RunWith(Parameterized.class)
public class DefaultFlowTestCase extends AbstractFlowConstructTestCase {

  private static final String FLOW_NAME = "test-flow";

  private DefaultFlowBuilder.DefaultFlow flow;
  private DefaultFlowBuilder.DefaultFlow stoppedFlow;
  private SensingNullMessageProcessor sensingMessageProcessor;
  private BiFunction<Processor, CoreEvent, CoreEvent> triggerFunction;

  @Rule
  public ExpectedException expectedException = none();

  public DefaultFlowTestCase(BiFunction<Processor, CoreEvent, CoreEvent> triggerFunction) {
    this.triggerFunction = triggerFunction;
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    BiFunction<Processor, CoreEvent, CoreEvent> blocking = (listener, event) -> just(event).transform(listener).block();
    BiFunction<Processor, CoreEvent, CoreEvent> async = (listener, event) -> {
      try {
        return listener.process(event);
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    };
    return asList(new Object[][] {{blocking}, {async}});
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    sensingMessageProcessor = getSensingNullMessageProcessor();

    List<Processor> processors = new ArrayList<>();
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("f")));
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("e")));
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("d")));
    processors.add(new StringAppendTransformer("a"));
    processors.add(new StringAppendTransformer("b"));
    processors.add(new StringAppendTransformer("c"));
    processors.add(event -> CoreEvent.builder(event).addVariable("thread", currentThread()).build());
    processors.add(sensingMessageProcessor);

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(processors)
        .build();

    stoppedFlow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(processors)
        .initialState(INITIAL_STATE_STOPPED)
        .build();
  }

  @Override
  protected AbstractFlowConstruct getFlowConstruct() throws Exception {
    return flow;
  }

  @Override
  protected AbstractFlowConstruct getStoppedFlowConstruct() throws Exception {
    return stoppedFlow;
  }

  @After
  public void after() throws MuleException {
    if (flow.isStarted()) {
      flow.stop();
    }

    if (flow.getLifecycleState().isInitialised()) {
      flow.dispose();
    }

    if (stoppedFlow.getLifecycleState().isInitialised()) {
      stoppedFlow.dispose();
    }
  }

  @Test
  public void testProcessOneWayEndpoint() throws Exception {
    flow.initialise();
    flow.start();
    CoreEvent event = eventBuilder(muleContext)
        .message(of(TEST_PAYLOAD))
        .build();
    CoreEvent response = triggerFunction.apply(directInboundMessageSource.getListener(), event);

    assertSucessfulProcessing((PrivilegedEvent) response);
  }

  @Test
  public void testProcessRequestResponseEndpoint() throws Exception {
    flow.initialise();
    flow.start();
    CoreEvent response = triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());

    assertSucessfulProcessing((PrivilegedEvent) response);
  }

  private void assertSucessfulProcessing(PrivilegedEvent response) throws MuleException {
    assertThat(response.getMessageAsString(muleContext), equalTo(TEST_PAYLOAD + "abcdef"));
    assertThat(response.getVariables().get("thread").getValue(), not(sameInstance(currentThread())));

    assertThat(((PrivilegedEvent) sensingMessageProcessor.event).getMessageAsString(muleContext),
               equalTo(TEST_PAYLOAD + "abc"));
    assertThat(sensingMessageProcessor.event.getVariables().get("thread").getValue(), not(sameInstance(currentThread())));
  }

  @Test
  public void testProcessStopped() throws Exception {
    flow.initialise();

    try {
      triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());
      fail("exception expected");
    } catch (Exception e) {
    }
  }

  @Test
  public void restartWithBlockingProcessingStrategy() throws Exception {
    after();

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(flow.getSource())
        .processors(flow.getProcessors())
        .processingStrategyFactory(new BlockingProcessingStrategyFactory())
        .build();

    flow.initialise();
    flow.start();

    flow.stop();
    flow.start();

    CoreEvent response = triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());
    assertThat(response, not(nullValue()));
  }

  @Test
  public void testFailStartingMessageSourceOnLifecycleShouldStopStartedPipelineProcesses() throws Exception {
    // Need to start mule context to have endpoints started during flow start
    muleContext.start();

    MessageSource mockMessageSource = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(new LifecycleException(mock(I18nMessage.class), mockMessageSource)).when(((Startable) mockMessageSource))
        .start();

    final List<Processor> processors = new ArrayList<>(flow.getProcessors());
    Processor mockMessageProcessor = spy(new LifecycleTrackerProcessor());
    processors.add(mockMessageProcessor);

    after();

    flow = (DefaultFlow) Flow.builder(FLOW_NAME, muleContext)
        .source(mockMessageSource)
        .processors(processors).build();

    flow.initialise();
    try {
      flow.start();
      fail();
    } catch (LifecycleException e) {
    }

    verify((Startable) mockMessageProcessor, times(1)).start();
    verify((Stoppable) mockMessageProcessor, times(1)).stop();

    verify((Startable) mockMessageSource, times(1)).start();
    verify((Stoppable) mockMessageSource, times(1)).stop();
  }

  @Test
  public void defaultMaxConcurrency() throws Exception {
    flow.initialise();
    flow.start();
    assertThat(flow.getMaxConcurrency(), equalTo(DEFAULT_MAX_CONCURRENCY));
    // When max concurrency is default Integer.MAX_VALUE then the Scheduler is created with no withMaxConcurrentTasks
    // configuration.
    verify(muleContext.getSchedulerService())
        .ioScheduler(eq(muleContext.getSchedulerBaseConfig().withName(flow.getName() + "." + BLOCKING.name())));
  }

  @Test
  public void customMaxConcurrency() throws Exception {
    int customMaxConcurrency = 1;
    Flow customFlow = Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(getSensingNullMessageProcessor())
        .maxConcurrency(customMaxConcurrency)
        .build();
    try {
      customFlow.initialise();
      customFlow.start();
      assertThat(customFlow.getMaxConcurrency(), equalTo(customMaxConcurrency));
      verify(muleContext.getSchedulerService())
          .ioScheduler(eq(muleContext.getSchedulerBaseConfig().withMaxConcurrentTasks(customMaxConcurrency)
              .withName(flow.getName() + "." + BLOCKING.name())));
      customFlow.stop();

    } finally {
      customFlow.dispose();
    }
  }

  @Test
  public void illegalCustomMaxConcurrency() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    Flow.builder(FLOW_NAME, muleContext)
        .source(directInboundMessageSource)
        .processors(getSensingNullMessageProcessor())
        .maxConcurrency(0)
        .build();
  }

}
