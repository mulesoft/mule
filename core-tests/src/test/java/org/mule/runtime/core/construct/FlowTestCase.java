/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.chain.DynamicMessageProcessorContainer;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.runtime.core.util.NotificationUtils.FlowMap;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Test;

public class FlowTestCase extends AbstractFlowConstructTestCase {

  private static final String FLOW_NAME = "test-flow";

  private Flow flow;
  private DynamicMessageProcessorContainer dynamicProcessorContainer;
  private SensingNullMessageProcessor sensingMessageProcessor;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    sensingMessageProcessor = getSensingNullMessageProcessor();

    flow = new Flow(FLOW_NAME, muleContext);
    flow.setMessageSource(directInboundMessageSource);

    dynamicProcessorContainer = mock(DynamicMessageProcessorContainer.class);
    when(dynamicProcessorContainer.process(any(Event.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return (Event) args[0];
    });
    doAnswer(invocation -> ((MessageProcessorPathElement) invocation.getArguments()[0]).addChild(dynamicProcessorContainer))
        .when(dynamicProcessorContainer).addMessageProcessorPathElements(any(MessageProcessorPathElement.class));
    List<Processor> processors = new ArrayList<>();
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("f")));
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("e")));
    processors.add(new ResponseMessageProcessorAdapter(new StringAppendTransformer("d")));
    processors.add(new StringAppendTransformer("a"));
    processors.add(new StringAppendTransformer("b"));
    processors.add(new StringAppendTransformer("c"));
    processors.add(dynamicProcessorContainer);
    processors.add(event -> {
      return Event.builder(event).addVariable("thread", Thread.currentThread()).build();
    });
    processors.add(sensingMessageProcessor);
    flow.setMessageProcessors(processors);
  }

  @Override
  protected void doTearDown() throws Exception {
    stopIfNeeded(muleContext.getRegistry().lookupObject(SchedulerService.class));
    super.doTearDown();
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
        .message(InternalMessage.of("hello"))
        .exchangePattern(ONE_WAY)
        .build();
    Event response = directInboundMessageSource.process(event);

    new PollingProber(50, 5).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        // While a SedaService returns null, a Flow echos the request when there is async hand-off
        assertEquals(event.getMessage(), response.getMessage());

        assertEquals("helloabc", sensingMessageProcessor.event.getMessageAsString(muleContext));
        assertNotSame(Thread.currentThread(), sensingMessageProcessor.event.getVariable("thread").getValue());
        return true;
      }
    });
  }

  @Test
  public void testProcessRequestResponseEndpoint() throws Exception {
    flow.initialise();
    flow.start();
    Event response = directInboundMessageSource.process(testEvent());

    assertEquals(TEST_PAYLOAD + "abcdef", response.getMessageAsString(muleContext));
    assertEquals(Thread.currentThread(), response.getVariable("thread").getValue());

    assertEquals(TEST_PAYLOAD + "abc", sensingMessageProcessor.event.getMessageAsString(muleContext));
    assertEquals(Thread.currentThread(), sensingMessageProcessor.event.getVariable("thread").getValue());

  }

  @Test
  public void processorPath() throws MuleException {
    flow.initialise();
    flow.start();

    Processor processorInSubflow = event -> event;

    assertThat(flow.getProcessorPath(sensingMessageProcessor), is("/test-flow/processors/8"));
    assertThat(flow.getProcessorPath(dynamicProcessorContainer), is("/test-flow/processors/6"));
    assertThat(flow.getProcessorPath(processorInSubflow), is(nullValue()));

    reset(dynamicProcessorContainer);
    FlowMap dynamicContainerFlowMap = mock(FlowMap.class);
    when(dynamicContainerFlowMap.resolvePath(processorInSubflow)).thenReturn("/sub_dyn/subprocessors/0");
    when(dynamicContainerFlowMap.getFlowMap())
        .thenReturn(Collections.singletonMap(processorInSubflow, "/sub_dyn/subprocessors/0"));
    when(dynamicProcessorContainer.buildInnerPaths()).thenReturn(dynamicContainerFlowMap);
    assertThat(flow.getProcessorPath(processorInSubflow), is("/sub_dyn/subprocessors/0"));
    verify(dynamicProcessorContainer, times(1)).buildInnerPaths();

    flow.getProcessorPath(processorInSubflow);
    // No new invocation, the dynamic container reference was initialized and removed from the pending dynamic containers list.
    verify(dynamicProcessorContainer, times(1)).buildInnerPaths();
  }

  @Test
  public void testProcessStopped() throws Exception {
    flow.initialise();

    try {
      directInboundMessageSource.process(testEvent());
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

    Event response = directInboundMessageSource.process(testEvent());
    assertThat(response, not(nullValue()));
  }

  @Test
  public void restartWithAsynchronousProcessingStrategy() throws Exception {
    flow.setProcessingStrategyFactory(new AsynchronousProcessingStrategyFactory());
    flow.initialise();
    flow.start();

    flow.stop();
    flow.start();

    Event response =
        directInboundMessageSource.process(eventBuilder().message(InternalMessage.of(TEST_PAYLOAD)).synchronous(false).build());
    assertThat(response, not(nullValue()));
  }

  @Test
  public void testDynamicPipeline() throws Exception {
    flow.initialise();
    flow.start();

    Processor appendPre = new StringAppendTransformer("1");
    Processor appendPost2 = new StringAppendTransformer("4");

    String pipelineId = flow.dynamicPipeline(null).injectBefore(appendPre, new StringAppendTransformer("2"))
        .injectAfter(new StringAppendTransformer("3"), appendPost2).resetAndUpdate();
    Event response = directInboundMessageSource.process(testEvent());
    assertEquals(TEST_PAYLOAD + "12abcdef34", response.getMessageAsString(muleContext));

    flow.dynamicPipeline(pipelineId).injectBefore(new StringAppendTransformer("2")).injectAfter(new StringAppendTransformer("3"))
        .resetAndUpdate();
    response = directInboundMessageSource.process(testEvent());
    assertEquals(TEST_PAYLOAD + "2abcdef3", response.getMessageAsString(muleContext));

    flow.dynamicPipeline(pipelineId).reset();
    response = directInboundMessageSource.process(testEvent());
    assertEquals(TEST_PAYLOAD + "abcdef", response.getMessageAsString(muleContext));
  }

  @Test
  public void testFailStartingMessageSourceOnLifecycleShouldStopStartedPipelineProcesses() throws Exception {
    // Need to start mule context to have endpoints started during flow start
    muleContext.start();

    MessageSource mockMessageSource = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(new LifecycleException(mock(I18nMessage.class), "Error starting component")).when(((Startable) mockMessageSource))
        .start();
    flow.setMessageSource(mockMessageSource);

    Processor mockMessageProcessor =
        mock(Processor.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
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
