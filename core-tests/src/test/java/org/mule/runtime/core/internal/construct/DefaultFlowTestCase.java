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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.message.Message.of;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.AbstractFlowConstructTestCase;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.test.core.lifecycle.LifecycleTrackerProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DefaultFlowTestCase extends AbstractFlowConstructTestCase {

  private static final String FLOW_NAME = "test-flow";

  private DefaultFlowBuilder.DefaultFlow flow;
  private SensingNullMessageProcessor sensingMessageProcessor;
  private BiFunction<Processor, Event, Event> triggerFunction;

  public DefaultFlowTestCase(BiFunction<Processor, Event, Event> triggerFunction) {
    this.triggerFunction = triggerFunction;
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    BiFunction<Processor, Event, Event> blocking = (listener, event) -> just(event).transform(listener).block();
    BiFunction<Processor, Event, Event> async = (listener, event) -> {
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

    flow = new DefaultFlowBuilder.DefaultFlow(FLOW_NAME, muleContext);
    flow.setMessageSource(directInboundMessageSource);

    List<Processor> processors = new ArrayList<>();
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("f")));
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("e")));
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("d")));
    processors.add(new StringAppendTransformer("a"));
    processors.add(new StringAppendTransformer("b"));
    processors.add(new StringAppendTransformer("c"));
    processors.add(event -> Event.builder(event).addVariable("thread", currentThread()).build());
    processors.add(sensingMessageProcessor);
    flow.setMessageProcessors(processors);
  }

  @Override
  protected AbstractFlowConstruct getFlowConstruct() throws Exception {
    return flow;
  }

  @After
  public void after() throws MuleException {
    if (flow.isStarted()) {
      flow.stop();
    }

    if (flow.getLifecycleState().isInitialised()) {
      flow.dispose();
    }
  }

  @Test
  public void testProcessOneWayEndpoint() throws Exception {
    flow.initialise();
    flow.start();
    Event event = eventBuilder()
        .message(of(TEST_PAYLOAD))
        .build();
    Event response = triggerFunction.apply(directInboundMessageSource.getListener(), event);

    assertSucessfulProcessing(response);
  }

  @Test
  public void testProcessRequestResponseEndpoint() throws Exception {
    flow.initialise();
    flow.start();
    Event response = triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());

    assertSucessfulProcessing(response);
  }

  private void assertSucessfulProcessing(Event response) throws MuleException {
    assertThat(response.getMessageAsString(muleContext), equalTo(TEST_PAYLOAD + "abcdef"));
    assertThat(response.getVariable("thread").getValue(), not(sameInstance(currentThread())));

    assertThat(sensingMessageProcessor.event.getMessageAsString(muleContext), equalTo(TEST_PAYLOAD + "abc"));
    assertThat(sensingMessageProcessor.event.getVariable("thread").getValue(), not(sameInstance(currentThread())));
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
  public void restartWithSynchronousProcessingStrategy() throws Exception {
    flow.setProcessingStrategyFactory(new SynchronousProcessingStrategyFactory());
    flow.initialise();
    flow.start();

    flow.stop();
    flow.start();

    Event response = triggerFunction.apply(directInboundMessageSource.getListener(), testEvent());
    assertThat(response, not(nullValue()));
  }

  @Test
  public void testFailStartingMessageSourceOnLifecycleShouldStopStartedPipelineProcesses() throws Exception {
    // Need to start mule context to have endpoints started during flow start
    muleContext.start();

    MessageSource mockMessageSource = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(new LifecycleException(mock(I18nMessage.class), "Error starting component")).when(((Startable) mockMessageSource))
        .start();
    flow.setMessageSource(mockMessageSource);

    Processor mockMessageProcessor = spy(new LifecycleTrackerProcessor());
    flow.getMessageProcessors().add(mockMessageProcessor);

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

}
