/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.compatibility.core.transformer.simple.OutboundAppendTransformer;
import org.mule.compatibility.core.transformer.simple.ResponseAppendTransformer;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;

import org.junit.Test;

/**
 * Tests flow of messages from {@link OutboundEndpoint#process(Event)} down to {@link AbstractMessageDispatcher} and the chain of
 * MessageProcessor's that implement the outbound endpoint processing.
 */
public class OutboundEndpointTestCase extends AbstractMessageProcessorTestCase {

  protected FakeMessageDispatcher dispacher;
  protected Event testOutboundEvent;

  @Test
  public void testDefaultFlowSync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(Event.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(Event.class))).then(echoEventAnswer);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(getTestFlow(muleContext));

    testOutboundEvent = createTestOutboundEvent();
    Event result = endpoint.process(testOutboundEvent);

    verify(reqTransformer, times(1)).process(any(Event.class));
    verify(resTransformer, times(1)).process(any(Event.class));

    assertMessageSentSame(true);
    assertEqualMessages(responseMessage, result.getMessage());
  }

  @Test
  public void testDefaultFlowAsync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(Event.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(Event.class))).then(echoEventAnswer);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, ONE_WAY, null);

    testOutboundEvent = createTestOutboundEvent();
    Event result = endpoint.process(testOutboundEvent);

    verify(reqTransformer, times(1)).process(any(Event.class));
    verify(resTransformer, never()).process(any(Event.class));

    dispacher.latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
    assertMessageSentSame(false);
    assertSame(testOutboundEvent, result);
  }

  @Test
  public void testSecurityFilterAccept() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(true), null, null, REQUEST_RESPONSE, null);

    testOutboundEvent = createTestOutboundEvent();
    Event result = endpoint.process(testOutboundEvent);

    assertMessageSentSame(true);
    assertEqualMessages(responseMessage, result.getMessage());
  }

  @Test
  public void testSecurityFilterNotAccept() throws Exception {
    TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
    muleContext.registerListener(securityNotificationListener);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(false), null, null, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(getTestFlow(muleContext));

    testOutboundEvent = createTestOutboundEvent();
    setCurrentEvent(testOutboundEvent);
    try {
      endpoint.process(testOutboundEvent);
      fail("Exception expected");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(TestSecurityFilter.StaticMessageUnauthorisedException.class)));
      endpoint.getFlowConstruct().getExceptionListener().handleException(e, testOutboundEvent);
    }

    assertMessageNotSent();

    assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
                 securityNotificationListener.securityNotification.getAction());
    assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
                 TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
  }

  @Test
  public void testSendNotfication() throws Exception {
    TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
    muleContext.registerListener(listener);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null,
                                                       REQUEST_RESPONSE, null);
    Event outboundEvent = createTestOutboundEvent();
    endpoint.process(outboundEvent);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(2, listener.messageNotificationList.size());
    assertEquals(EndpointMessageNotification.MESSAGE_SEND_BEGIN, listener.messageNotificationList.get(0).getAction());
    assertEquals(EndpointMessageNotification.MESSAGE_SEND_END, listener.messageNotificationList.get(1).getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(0).getEndpoint());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(1).getEndpoint());
    assertTrue(listener.messageNotificationList.get(0).getSource() instanceof InternalMessage);
    assertTrue(listener.messageNotificationList.get(1).getSource() instanceof InternalMessage);
    assertThat(listener.messageNotificationList.get(0).getSource().getPayload().getValue(),
               equalTo(outboundEvent.getMessage().getPayload().getValue()));
    assertEquals(RESPONSE_MESSAGE,
                 listener.messageNotificationList.get(1).getSource().getPayload().getValue());
  }

  @Test
  public void testDispatchNotfication() throws Exception {
    TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
    muleContext.registerListener(listener);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, ONE_WAY, null);
    Event outboundEvent = createTestOutboundEvent();
    endpoint.process(outboundEvent);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(2, listener.messageNotificationList.size());
    assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN, listener.messageNotificationList.get(0).getAction());
    assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_END, listener.messageNotificationList.get(1).getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(0).getEndpoint());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(1).getEndpoint());
    assertTrue(listener.messageNotificationList.get(0).getSource() instanceof InternalMessage);
    assertTrue(listener.messageNotificationList.get(1).getSource() instanceof InternalMessage);
    assertThat(listener.messageNotificationList.get(0).getSource().getPayload().getValue(),
               equalTo(outboundEvent.getMessage().getPayload().getValue()));
    assertThat(listener.messageNotificationList.get(1).getSource().getPayload().getValue(),
               equalTo(outboundEvent.getMessage().getPayload().getValue()));
  }

  @Test
  public void testTransformers() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, new OutboundAppendTransformer(),
                                                       new ResponseAppendTransformer(), REQUEST_RESPONSE, null);
    Event outboundEvent = createTestOutboundEvent();
    Event result = endpoint.process(outboundEvent);

    assertMessageSent(true);

    assertEquals(TEST_MESSAGE + OutboundAppendTransformer.APPEND_STRING,
                 dispacher.sensedSendEvent.getMessageAsString(muleContext));

    assertNotNull(result);
    assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING, result.getMessageAsString(muleContext));
  }

  @Test
  public void testConnectorNotStarted() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null,
                                                       REQUEST_RESPONSE, null);
    testOutboundEvent = createTestOutboundEvent();
    endpoint.getConnector().stop();

    try {
      endpoint.process(testOutboundEvent);
      fail("Exception expected");
    } catch (MessagingException e) {
      // expected
    }
  }

  @Test
  public void testObjectAwareInjection() throws Exception {
    EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
    endpointBuilder.addMessageProcessor(new ObjectAwareProcessor());

    OutboundEndpoint endpoint = endpointBuilder.buildOutboundEndpoint();
    endpoint.process(createTestOutboundEvent());

    ObjectAwareProcessor objectAware = (ObjectAwareProcessor) endpoint.getMessageProcessors().get(0);

    assertEquals(muleContext, objectAware.context);
    assertEquals(endpoint, objectAware.endpoint);
  }

  protected Event assertMessageSent(boolean sync) throws MuleException {
    Event event;
    if (sync) {
      assertNull(dispacher.sensedDispatchEvent);
      assertNotNull(dispacher.sensedSendEvent);
      event = dispacher.sensedSendEvent;
    } else {
      assertNull(dispacher.sensedSendEvent);
      assertNotNull(dispacher.sensedDispatchEvent);
      event = dispacher.sensedDispatchEvent;
    }
    assertNotNull(event.getMessage());
    return event;
  }

  protected Event assertMessageSentSame(boolean sync) throws MuleException {
    Event event = assertMessageSent(sync);
    assertEquals(TEST_MESSAGE, event.getMessageAsString(muleContext));
    assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
    return event;
  }

  protected void assertMessageNotSent() throws MuleException {
    assertNull(dispacher.sensedSendEvent);
    assertNull(dispacher.sensedDispatchEvent);
  }

  protected void assertEqualMessages(InternalMessage expect, InternalMessage actual) {
    assertThat(actual.getPayload().getValue(), equalTo(expect.getPayload().getValue()));
    assertEquals(expect.getPayload().getDataType(), actual.getPayload().getDataType());
  }

  protected OutboundEndpoint createOutboundEndpoint(String uri, Filter filter,
                                                    SecurityFilter securityFilter,
                                                    Transformer in,
                                                    Transformer response,
                                                    MessageExchangePattern exchangePattern,
                                                    TransactionConfig txConfig)
      throws Exception {

    OutboundEndpoint endpoint = createTestOutboundEndpoint(uri, filter, securityFilter, in, response,
                                                           exchangePattern, txConfig);
    dispacher = new FakeMessageDispatcher(endpoint);
    Connector connector = endpoint.getConnector();
    connector.setDispatcherFactory(new TestMessageDispatcherFactory() {

      @Override
      public MessageDispatcher create(OutboundEndpoint ep) throws MuleException {
        return dispacher;
      }
    });
    return endpoint;
  }

  protected OutboundEndpoint createOutboundEndpoint(Filter filter,
                                                    SecurityFilter securityFilter,
                                                    Transformer in,
                                                    Transformer response,
                                                    MessageExchangePattern exchangePattern,
                                                    TransactionConfig txConfig)
      throws Exception {
    return createOutboundEndpoint("test://test", filter, securityFilter, in, response, exchangePattern, txConfig);

  }

  static class FakeMessageDispatcher extends TestMessageDispatcher {

    Latch latch = new Latch();
    Event sensedSendEvent;
    Event sensedDispatchEvent;

    public FakeMessageDispatcher(OutboundEndpoint endpoint) {
      super(endpoint);
    }

    @Override
    protected InternalMessage doSend(Event event) throws Exception {
      sensedSendEvent = event;
      latch.countDown();
      return responseMessage;
    }

    @Override
    protected void doDispatch(Event event) throws Exception {
      sensedDispatchEvent = event;
      latch.countDown();
    }

  }

}
