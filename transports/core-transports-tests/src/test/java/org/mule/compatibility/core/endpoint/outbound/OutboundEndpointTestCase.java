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
import static org.hamcrest.CoreMatchers.nullValue;
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
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.compatibility.core.transformer.simple.OutboundAppendTransformer;
import org.mule.compatibility.core.transformer.simple.ResponseAppendTransformer;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;

import org.junit.Test;

/**
 * Tests flow of messages from {@link OutboundEndpoint#process(MuleEvent)} down to {@link AbstractMessageDispatcher} and the chain
 * of MessageProcessor's that implement the outbound endpoint processing.
 */
public class OutboundEndpointTestCase extends AbstractMessageProcessorTestCase {

  protected FakeMessageDispatcher dispacher;
  protected MuleEvent testOutboundEvent;

  @Test
  public void testDefaultFlowSync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(getTestFlow());

    testOutboundEvent = createTestOutboundEvent();
    MuleEvent result = endpoint.process(testOutboundEvent);

    verify(reqTransformer, times(1)).process(any(MuleEvent.class));
    verify(resTransformer, times(1)).process(any(MuleEvent.class));

    assertMessageSentSame(true);
    assertEqualMessages(responseMessage, result.getMessage());
  }

  @Test
  public void testDefaultFlowNonBlocking() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);

    final Flow flow = new Flow("", muleContext);
    flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
    muleContext.getRegistry().registerFlowConstruct(flow);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(flow);

    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    MuleEvent event = getNonBlockingTestEventUsingFlow(TEST_MESSAGE, nullReplyToHandler, flow);

    MuleEvent response = endpoint.process(event);
    assertThat(response, equalTo(NonBlockingVoidMuleEvent.getInstance()));

    assertThat(getNonBlockingResponse(nullReplyToHandler, response).getMessage().getPayload(),
               equalTo(event.getMessage().getPayload()));
    verify(reqTransformer, times(1)).process(any());
    verify(resTransformer, times(1)).process(any());
  }

  @Test
  public void testDefaultFlowNonBlockingError() throws Exception {
    final Flow flow = new Flow("", muleContext);
    flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
    muleContext.getRegistry().registerFlowConstruct(flow);

    OutboundEndpoint endpoint = createOutboundEndpoint("test://AlwaysFail", null, null, null, null, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(flow);
    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    MuleEvent event = getNonBlockingTestEventUsingFlow(TEST_MESSAGE, nullReplyToHandler, flow);

    MuleEvent response = endpoint.process(event);
    assertThat(response, equalTo(NonBlockingVoidMuleEvent.getInstance()));

    try {
      getNonBlockingResponse(nullReplyToHandler, response);
      fail("Exception Expected");
    } catch (Exception e) {
      assertThat(e, instanceOf(MessagingException.class));
      assertThat(e.getCause(), instanceOf(RoutingException.class));
    }

    assertThat(nullReplyToHandler.event, is(nullValue()));
  }

  protected MuleEvent getNonBlockingResponse(SensingNullReplyToHandler replyToHandler, MuleEvent result) throws Exception {
    if (NonBlockingVoidMuleEvent.getInstance() == result) {
      if (!replyToHandler.latch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
        throw new RuntimeException("No Non-Blocking Response");
      }
      if (replyToHandler.exception != null) {
        throw replyToHandler.exception;
      }
      return replyToHandler.event;
    } else {
      return result;
    }
  }

  @Test
  public void testDefaultFlowAsync() throws Exception {
    Transformer reqTransformer = mock(Transformer.class);
    when(reqTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);
    Transformer resTransformer = mock(Transformer.class);
    when(resTransformer.process(any(MuleEvent.class))).then(echoEventAnswer);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, ONE_WAY, null);

    testOutboundEvent = createTestOutboundEvent();
    MuleEvent result = endpoint.process(testOutboundEvent);

    verify(reqTransformer, times(1)).process(any(MuleEvent.class));
    verify(resTransformer, never()).process(any(MuleEvent.class));

    dispacher.latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
    assertMessageSentSame(false);
    assertSame(VoidMuleEvent.getInstance(), result);
  }

  @Test
  public void testSecurityFilterAccept() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(true), null, null, REQUEST_RESPONSE, null);

    testOutboundEvent = createTestOutboundEvent();
    MuleEvent result = endpoint.process(testOutboundEvent);

    assertMessageSentSame(true);
    assertEqualMessages(responseMessage, result.getMessage());
  }

  @Test
  public void testSecurityFilterNotAccept() throws Exception {
    TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
    muleContext.registerListener(securityNotificationListener);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(false), null, null, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(getTestFlow());

    testOutboundEvent = createTestOutboundEvent();
    setCurrentEvent(testOutboundEvent);
    try {
      endpoint.process(testOutboundEvent);
      fail("Exception expected");
    } catch (TestSecurityFilter.StaticMessageUnauthorisedException e) {
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
    MuleEvent outboundEvent = createTestOutboundEvent();
    endpoint.process(outboundEvent);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(2, listener.messageNotificationList.size());
    assertEquals(EndpointMessageNotification.MESSAGE_SEND_BEGIN, listener.messageNotificationList.get(0).getAction());
    assertEquals(EndpointMessageNotification.MESSAGE_SEND_END, listener.messageNotificationList.get(1).getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(0).getEndpoint());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(1).getEndpoint());
    assertTrue(listener.messageNotificationList.get(0).getSource() instanceof MuleMessage);
    assertTrue(listener.messageNotificationList.get(1).getSource() instanceof MuleMessage);
    assertThat(listener.messageNotificationList.get(0).getSource().getPayload(),
               equalTo(outboundEvent.getMessage().getPayload()));
    assertEquals(RESPONSE_MESSAGE,
                 listener.messageNotificationList.get(1).getSource().getPayload());
  }

  @Test
  public void testDispatchNotfication() throws Exception {
    TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
    muleContext.registerListener(listener);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, ONE_WAY, null);
    MuleEvent outboundEvent = createTestOutboundEvent();
    endpoint.process(outboundEvent);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(2, listener.messageNotificationList.size());
    assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN, listener.messageNotificationList.get(0).getAction());
    assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_END, listener.messageNotificationList.get(1).getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(0).getEndpoint());
    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 listener.messageNotificationList.get(1).getEndpoint());
    assertTrue(listener.messageNotificationList.get(0).getSource() instanceof MuleMessage);
    assertTrue(listener.messageNotificationList.get(1).getSource() instanceof MuleMessage);
    assertThat(listener.messageNotificationList.get(0).getSource().getPayload(),
               equalTo(outboundEvent.getMessage().getPayload()));
    assertThat(listener.messageNotificationList.get(1).getSource().getPayload(),
               equalTo(outboundEvent.getMessage().getPayload()));
  }

  @Test
  public void testTransformers() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, new OutboundAppendTransformer(),
                                                       new ResponseAppendTransformer(), REQUEST_RESPONSE, null);
    MuleEvent outboundEvent = createTestOutboundEvent();
    MuleEvent result = endpoint.process(outboundEvent);

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

  protected MuleEvent assertMessageSent(boolean sync) throws MuleException {
    MuleEvent event;
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

  protected MuleEvent assertMessageSentSame(boolean sync) throws MuleException {
    MuleEvent event = assertMessageSent(sync);
    if (sync) {
      // We can't assert this for async because event gets rewritten
      assertEquals(testOutboundEvent, event);
    }
    assertEquals(TEST_MESSAGE, event.getMessageAsString(muleContext));
    assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
    return event;
  }

  protected void assertMessageNotSent() throws MuleException {
    assertNull(dispacher.sensedSendEvent);
    assertNull(dispacher.sensedDispatchEvent);
  }

  protected void assertEqualMessages(MuleMessage expect, MuleMessage actual) {
    assertThat(actual.getPayload(), equalTo(expect.getPayload()));
    assertEquals(expect.getDataType(), actual.getDataType());
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
    MuleEvent sensedSendEvent;
    MuleEvent sensedDispatchEvent;

    public FakeMessageDispatcher(OutboundEndpoint endpoint) {
      super(endpoint);
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception {
      sensedSendEvent = event;
      latch.countDown();
      return responseMessage;
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception {
      sensedDispatchEvent = event;
      latch.countDown();
    }

    @Override
    protected void doSendNonBlocking(MuleEvent event, CompletionHandler<MuleMessage, Exception, Void> completionHandler) {
      sensedSendEvent = event;
      latch.countDown();
      super.doSendNonBlocking(event, completionHandler);
    }
  }

}
