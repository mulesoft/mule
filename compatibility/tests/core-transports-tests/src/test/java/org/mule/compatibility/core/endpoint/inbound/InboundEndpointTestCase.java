/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.createEventUsingInboundEndpoint;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.createErrorMock;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.security.EndpointSecurityFilter;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.endpoint.AbstractEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.compatibility.core.transformer.simple.InboundAppendTransformer;
import org.mule.compatibility.core.transformer.simple.ResponseAppendTransformer;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.processor.NullMessageProcessor;
import org.mule.tck.security.TestSecurityFilter;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class InboundEndpointTestCase extends AbstractMessageProcessorTestCase {

  private static final String TEST_MESSAGE = "test";
  private InboundEndpoint endpoint;
  private SensingNullMessageProcessor inboundListener;
  private InternalMessage inMessage;
  private Event requestEvent;
  private Event responseEvent;
  private Event result;

  private static String RESPONSE_MESSAGE = "response-message";

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    inMessage = createTestRequestMessage();
    inboundListener = new SensingNullMessageProcessor();
  }

  @Test
  public void testDefaultFlowSync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(Event.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(Event.class))).then(echoEventAnswer);

    endpoint = createTestInboundEndpoint(null, null, reqTransformer, resTransformer, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    verify(reqTransformer, times(1)).process(any(Event.class));
    verify(resTransformer, times(1)).process(any(Event.class));

    assertMessageSentSame();
    assertEquals(responseEvent.getMessage(), result.getMessage());
  }

  @Test
  public void testDefaultFlowAsync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(Event.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(Event.class))).then(echoEventAnswer);

    endpoint = createTestInboundEndpoint(null, null, reqTransformer, resTransformer, ONE_WAY, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    verify(reqTransformer, times(1)).process(any(Event.class));
    verify(resTransformer, never()).process(any(Event.class));

    assertMessageSentSame();
    assertEquals(responseEvent.getMessage(), result.getMessage());
  }

  @Test
  public void testFilterAccept() throws Exception {
    endpoint = createTestInboundEndpoint(new TestFilter(true), null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSentSame();
    assertEquals(responseEvent.getMessage(), result.getMessage());

  }

  @Test
  public void testFilterNotAccept() throws Exception {
    endpoint = createTestInboundEndpoint(new TestFilter(false), null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    try {
      result = mpChain.process(requestEvent);
      fail("Filter should have thrown a FilterException");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(FilterUnacceptedException.class)));
    }

    assertMessageNotSent();
  }

  @Test
  public void testSecurityFilterAccept() throws Exception {
    endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(true), null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSentSame();
    assertEquals(responseEvent.getMessage(), result.getMessage());

  }

  @Test
  public void testSecurityFilterNotAccept() throws Exception {
    TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
    muleContext.registerListener(securityNotificationListener);

    endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    // Required for UnauthorisedException creation
    setCurrentEvent(requestEvent);

    try {
      result = mpChain.process(requestEvent);
      fail("Exception expected");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(TestSecurityFilter.StaticMessageUnauthorisedException.class)));
      endpoint.getFlowConstruct().getExceptionListener().handleException(e, requestEvent);
    }

    assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
                 securityNotificationListener.securityNotification.getAction());
    assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
                 TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
  }

  /**
   * Assert that {@link EndpointSecurityFilter} is only invoked if endpoint {@link Filter} accepts message.
   */
  @Test
  public void testFilterFirstThenSecurityFilter() throws Exception {
    TestSecurityFilter securityFilter = new TestSecurityFilter(false);
    endpoint = createTestInboundEndpoint(new TestFilter(false), securityFilter, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    try {
      result = mpChain.process(requestEvent);
      fail("Filter should have thrown a FilterException");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(FilterUnacceptedException.class)));
    }

    assertFalse(securityFilter.wasCalled());
    assertMessageNotSent();
  }

  @Test
  public void testMessagePropertyErrorMapping() throws Exception {
    endpoint = createTestInboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);
    RuntimeException exception = new RuntimeException();
    responseEvent = Event.builder(responseEvent)
        .message(InternalMessage.builder(responseEvent.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception))
            .build())
        .error(createErrorMock(exception)).build();

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSentSame();
    assertThat(result.getMessage().getPayload().getValue(), equalTo(responseEvent.getMessage().getPayload().getValue()));
    final int status = result.getMessage().getOutboundProperty("status", 0);
    assertEquals(500, status);
  }

  @Test
  public void testResponseTransformerExceptionDetailAfterRequestFlowInterupt() throws Exception {
    endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null, new ResponseAppendTransformer(),
                                         REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);
    responseEvent = Event.builder(createTestResponseEvent(endpoint)).message(InternalMessage.builder(responseEvent.getMessage())
        .exceptionPayload(new DefaultExceptionPayload(new RuntimeException()))
        .build()).build();

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    // Required for UnauthorisedException creation
    setCurrentEvent(requestEvent);

    try {
      result = mpChain.process(requestEvent);
      fail("Exception expected");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(TestSecurityFilter.StaticMessageUnauthorisedException.class)));
    }

    assertMessageNotSent();
  }

  @Test
  public void testNotfication() throws Exception {
    TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
    muleContext.registerListener(listener);

    endpoint = createTestInboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotification.getEndpoint());
    assertTrue(listener.messageNotification.getSource() instanceof InternalMessage);
    assertThat(listener.messageNotification.getSource().getPayload().getValue(), equalTo(inMessage.getPayload().getValue()));
  }

  @Test
  public void testTransformers() throws Exception {
    endpoint = createTestInboundEndpoint(null, null, new InboundAppendTransformer(), new ResponseAppendTransformer(),
                                         REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    Processor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSent();
    assertEquals(TEST_MESSAGE + InboundAppendTransformer.APPEND_STRING,
                 inboundListener.sensedEvent.getMessageAsString(muleContext));

    assertNotNull(result);
    assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
                 getPayloadAsString(result.getMessage()));
  }

  @Test
  public void testObjectAwareInjection() throws Exception {
    EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
    endpointBuilder.addMessageProcessor(new ObjectAwareProcessor());

    endpoint = endpointBuilder.buildInboundEndpoint();
    endpoint.setListener(new NullMessageProcessor());
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    endpoint.start();

    ObjectAwareProcessor objectAware = (ObjectAwareProcessor) endpoint.getMessageProcessors().get(0);

    assertEquals(muleContext, objectAware.context);
    assertEquals(endpoint, objectAware.endpoint);

    endpoint.stop();
  }

  protected InternalMessage createTestRequestMessage() {
    return InternalMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("prop1", "value1").build();
  }

  protected Event createTestRequestEvent(InboundEndpoint ep) throws Exception {
    return createEventUsingInboundEndpoint(eventBuilder(), inMessage, ep);
  }

  protected Event createTestResponseEvent(InboundEndpoint ep) throws Exception {
    return createEventUsingInboundEndpoint(eventBuilder(), InternalMessage.builder().payload(RESPONSE_MESSAGE).build(), ep);
  }

  protected Event assertMessageSent() throws MuleException {
    Event event = inboundListener.sensedEvent;
    assertNotNull(event);
    assertNotNull(event.getMessage());
    return event;
  }

  protected Event assertMessageSentSame() throws MuleException {
    assertMessageSent();
    Event event = inboundListener.sensedEvent;
    assertThat(event.getMessage().getPayload().getValue(), equalTo(requestEvent.getMessage().getPayload().getValue()));
    assertEquals(TEST_MESSAGE, event.getMessageAsString(muleContext));
    assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
    return event;
  }

  protected void assertMessageNotSent() throws MuleException {
    Event event = inboundListener.sensedEvent;
    assertNull(event);
  }

  private class SensingNullMessageProcessor implements Processor {

    Event sensedEvent;

    @Override
    public Event process(Event event) throws MuleException {
      sensedEvent = event;
      return responseEvent;
    }
  }
}
