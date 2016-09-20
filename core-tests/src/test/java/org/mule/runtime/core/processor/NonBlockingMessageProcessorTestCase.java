/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.getCurrentEvent;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class NonBlockingMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private NonBlockingMessageProcessor nonBlockingMessageProcessor = new TestNonBlockingProcessor();

  @Test
  public void blockingProcess() throws MuleException, Exception {
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    Event request = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_MESSAGE))
        .build();
    Event response = nonBlockingMessageProcessor.process(request);

    // Test processor echos request so we can assert request equals response.
    assertThat(response, equalTo(request));
  }

  @Test
  public void nonBlockingProcessVoidMuleEventResponse() throws MuleException, Exception {
    Event request = createNonBlockingTestEvent();
    Event response = nonBlockingMessageProcessor.process(request);

    assertThat(response, is(instanceOf(NonBlockingVoidMuleEvent.class)));
  }

  @Test
  public void clearRequestContextAfterNonBlockingProcess() throws MuleException, Exception {
    Event request = createNonBlockingTestEvent();
    setCurrentEvent(request);
    nonBlockingMessageProcessor.process(request);

    assertThat(getCurrentEvent(), is(nullValue()));
  }

  private Event createNonBlockingTestEvent() throws Exception {
    Flow flow = MuleTestUtils.getTestFlow(muleContext);
    flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
    return Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.builder().payload(TEST_MESSAGE).build())
        .exchangePattern(REQUEST_RESPONSE).replyToHandler(new SensingNullReplyToHandler()).flow(flow).build();
  }

  private class TestNonBlockingProcessor extends AbstractNonBlockingMessageProcessor {

    @Override
    protected Event processBlocking(Event event) throws MuleException {
      return event;
    }

    @Override
    public void processNonBlocking(final Event event, CompletionHandler completionHandler) {}
  }
}


