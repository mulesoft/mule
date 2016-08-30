/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
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
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.createErrorMock;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.security.EndpointSecurityFilter;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.endpoint.AbstractEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.compatibility.core.transformer.simple.InboundAppendTransformer;
import org.mule.compatibility.core.transformer.simple.ResponseAppendTransformer;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.processor.NullMessageProcessor;
import org.mule.tck.security.TestSecurityFilter;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class InboundEndpointTestCase extends AbstractMessageProcessorTestCase {

  private static final String TEST_MESSAGE = "test";
  private InboundEndpoint endpoint;
  private SensingNullMessageProcessor inboundListener;
  private MuleMessage inMessage;
  private MuleEvent requestEvent;
  private MuleEvent responseEvent;
  private MuleEvent result;

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
    when(reqTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);

    endpoint = createTestInboundEndpoint(null, null, reqTransformer, resTransformer, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    verify(reqTransformer, times(1)).process(any(MuleEvent.class));
    verify(resTransformer, times(1)).process(any(MuleEvent.class));

    assertMessageSentSame(true);
    assertEquals(responseEvent.getMessage(), result.getMessage());
  }

  @Test
  public void testDefaultFlowAsync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);

    endpoint = createTestInboundEndpoint(null, null, reqTransformer, resTransformer, ONE_WAY, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    verify(reqTransformer, times(1)).process(any(MuleEvent.class));
    verify(resTransformer, never()).process(any(MuleEvent.class));

    assertMessageSentSame(false);
    assertEquals(responseEvent.getMessage(), result.getMessage());
  }

  @Test
  public void testFilterAccept() throws Exception {
    endpoint = createTestInboundEndpoint(new TestFilter(true), null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSentSame(true);
    assertEquals(responseEvent.getMessage(), result.getMessage());

  }

  @Test
  public void testFilterNotAccept() throws Exception {
    endpoint = createTestInboundEndpoint(new TestFilter(false), null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    try {
      result = mpChain.process(requestEvent);
      fail("Filter should have thrown a FilterException");
    } catch (FilterUnacceptedException e) {
      // expected
    }

    assertMessageNotSent();
  }

  @Test
  public void testSecurityFilterAccept() throws Exception {
    endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(true), null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSentSame(true);
    assertEquals(responseEvent.getMessage(), result.getMessage());

  }

  @Test
  public void testSecurityFilterNotAccept() throws Exception {
    TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
    muleContext.registerListener(securityNotificationListener);

    endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    // Required for UnauthorisedException creation
    setCurrentEvent(requestEvent);

    try {
      result = mpChain.process(requestEvent);
      fail("Exception expected");
    } catch (TestSecurityFilter.StaticMessageUnauthorisedException e) {
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
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    try {
      result = mpChain.process(requestEvent);
      fail("Filter should have thrown a FilterException");
    } catch (FilterUnacceptedException e) {
      // expected
    }

    assertFalse(securityFilter.wasCalled());
    assertMessageNotSent();
  }

  @Test
  public void testMessagePropertyErrorMapping() throws Exception {
    endpoint = createTestInboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);
    RuntimeException exception = new RuntimeException();
    responseEvent = MuleEvent.builder(responseEvent)
        .message(MuleMessage.builder(responseEvent.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
        .error(createErrorMock(exception)).build();

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSentSame(true);
    assertEquals(responseEvent.getMessage(), result.getMessage());
    final int status = result.getMessage().getOutboundProperty("status", 0);
    assertEquals(500, status);
  }

  @Test
  public void testResponseTransformerExceptionDetailAfterRequestFlowInterupt() throws Exception {
    endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null, new ResponseAppendTransformer(),
                                         REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);
    responseEvent.setMessage(MuleMessage.builder(responseEvent.getMessage())
        .exceptionPayload(new DefaultExceptionPayload(new RuntimeException()))
        .build());

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());

    // Required for UnauthorisedException creation
    setCurrentEvent(requestEvent);

    try {
      result = mpChain.process(requestEvent);
      fail("Exception expected");
    } catch (TestSecurityFilter.StaticMessageUnauthorisedException e) {
      // expected
    }

    assertMessageNotSent();
  }

  @Test
  public void testNotfication() throws Exception {
    TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
    muleContext.registerListener(listener);

    endpoint = createTestInboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotification.getEndpoint());
    assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
    assertThat(listener.messageNotification.getSource().getPayload(), equalTo(inMessage.getPayload()));
  }

  @Test
  public void testTransformers() throws Exception {
    endpoint = createTestInboundEndpoint(null, null, new InboundAppendTransformer(), new ResponseAppendTransformer(),
                                         REQUEST_RESPONSE, null);
    endpoint.setListener(inboundListener);
    endpoint.setFlowConstruct(getTestFlow());
    requestEvent = createTestRequestEvent(endpoint);
    responseEvent = createTestResponseEvent(endpoint);

    MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(endpoint.getFlowConstruct());
    result = mpChain.process(requestEvent);

    assertMessageSent(true);
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
    endpoint.setFlowConstruct(getTestFlow());
    endpoint.start();

    ObjectAwareProcessor objectAware = (ObjectAwareProcessor) endpoint.getMessageProcessors().get(0);

    assertEquals(muleContext, objectAware.context);
    assertEquals(endpoint, objectAware.endpoint);

    endpoint.stop();
  }

  protected MuleMessage createTestRequestMessage() {
    return MuleMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("prop1", "value1").build();
  }

  protected MuleEvent createTestRequestEvent(InboundEndpoint ep) throws Exception {
    Flow flow = getTestFlow();
    final MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(inMessage).flow(flow)
        .session(getTestSession(null, muleContext)).build();
    populateFieldsFromInboundEndpoint(event, ep);
    return event;
  }

  protected MuleEvent createTestResponseEvent(InboundEndpoint ep) throws Exception {
    Flow flow = getTestFlow();
    final MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR))
        .message(MuleMessage.builder().payload(RESPONSE_MESSAGE).build()).flow(flow).session(getTestSession(null, muleContext))
        .build();
    populateFieldsFromInboundEndpoint(event, ep);
    return event;
  }

  protected MuleEvent assertMessageSent(boolean sync) throws MuleException {
    MuleEvent event = inboundListener.sensedEvent;
    assertNotNull(event);
    assertEquals(sync, event.getExchangePattern().hasResponse());
    assertNotNull(event.getMessage());
    return event;
  }

  protected MuleEvent assertMessageSentSame(boolean sync) throws MuleException {
    assertMessageSent(sync);
    MuleEvent event = inboundListener.sensedEvent;
    assertEquals(requestEvent, event);
    assertEquals(TEST_MESSAGE, event.getMessageAsString(muleContext));
    assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
    return event;
  }

  protected void assertMessageNotSent() throws MuleException {
    MuleEvent event = inboundListener.sensedEvent;
    assertNull(event);
  }

  private class SensingNullMessageProcessor implements MessageProcessor {

    MuleEvent sensedEvent;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      sensedEvent = event;
      return responseEvent;
    }
  }
}
