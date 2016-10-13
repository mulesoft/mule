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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.core.context.notification.EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN;
import static org.mule.compatibility.core.context.notification.EndpointMessageNotification.MESSAGE_DISPATCH_END;
import static org.mule.compatibility.core.context.notification.EndpointMessageNotification.MESSAGE_SEND_BEGIN;
import static org.mule.compatibility.core.context.notification.EndpointMessageNotification.MESSAGE_SEND_END;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupServiceDescriptor;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.registry.LegacyServiceType;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.compatibility.core.endpoint.DynamicOutboundEndpoint;
import org.mule.compatibility.core.endpoint.DynamicURIBuilder;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.compatibility.core.transformer.simple.OutboundAppendTransformer;
import org.mule.compatibility.core.transformer.simple.ResponseAppendTransformer;
import org.mule.compatibility.core.transport.service.DefaultTransportServiceDescriptor;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;

import java.util.Properties;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.functors.NOPClosure;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * Tests flow of messages from
 * {@link org.mule.compatibility.core.endpoint.DynamicOutboundEndpoint#process(org.mule.Event.MuleEvent)} down to
 * {@link org.mule.compatibility.core.transport.AbstractMessageDispatcher} and the chain of MessageProcessor's that implement the
 * outbound endpoint processing.
 */
public class DynamicOutboundEndpointTestCase extends AbstractMessageProcessorTestCase {

  private Event testOutboundEvent;

  @Before
  public void setCurrentTestInstance() throws ServiceException {
    overrideDispatcherInServiceDescriptor();
    MyMessageDispatcherFactory.dispatcher = null;
  }

  @Before
  public void clearAssertionClosures() {
    MyMessageDispatcherFactory.afterSend = NOPClosure.getInstance();
    MyMessageDispatcherFactory.afterDispatch = NOPClosure.getInstance();
  }

  @Test
  public void testDefaultFlowRequestResponse() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);
    testOutboundEvent = createTestOutboundEvent();

    Event result = endpoint.process(testOutboundEvent);

    assertEventSent();
    assertEqualMessages(responseMessage, result.getMessage());
  }

  @Test
  public void testDefaultFlowOneWay() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, ONE_WAY, null);
    assertTrue(endpoint instanceof DynamicOutboundEndpoint);

    testOutboundEvent = createTestOutboundEvent();

    Event result = endpoint.process(testOutboundEvent);

    assertEventDispatched();
    assertSame(testOutboundEvent, result);
    assertMessageSentEqual(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent);
  }

  @Test
  public void testSecurityFilterAccept() throws Exception {
    OutboundEndpoint endpoint =
        createOutboundEndpoint(null, new TestSecurityFilter(true), null, null, REQUEST_RESPONSE, null);
    testOutboundEvent = createTestOutboundEvent();

    Event result = endpoint.process(testOutboundEvent);

    assertEventSent();
    assertMessageSentEqual(MyMessageDispatcherFactory.dispatcher.sensedSendEvent);

    assertEqualMessages(responseMessage, result.getMessage());
  }

  @Test
  public void testSecurityFilterNotAccept() throws Exception {
    TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
    muleContext.registerListener(securityNotificationListener);

    OutboundEndpoint endpoint =
        createOutboundEndpoint(null, new TestSecurityFilter(false), null, null, REQUEST_RESPONSE, null);
    endpoint.setFlowConstruct(getTestFlow(muleContext));
    testOutboundEvent = createTestOutboundEvent();

    try {
      endpoint.process(testOutboundEvent);
      fail("Exception expected");
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(TestSecurityFilter.StaticMessageUnauthorisedException.class)));
      endpoint.getFlowConstruct().getExceptionListener().handleException(e, testOutboundEvent);
    }

    assertNull(MyMessageDispatcherFactory.dispatcher);

    assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
                 securityNotificationListener.securityNotification.getAction());
    assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
                 TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
  }

  @Test
  public void testSendNotification() throws Exception {
    final TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
    muleContext.registerListener(listener);

    final Event outboundEvent = createTestOutboundEvent();

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);

    MyMessageDispatcherFactory.afterSend = input -> {
      try {
        assertTrue(listener.latchFirst.await(RECEIVE_TIMEOUT, MILLISECONDS));
      } catch (InterruptedException e) {
        fail(e.getMessage());
      }
      assertThat(listener.messageNotificationList, hasSize(1));
      assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_SEND_BEGIN));
      assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(InternalMessage.class));
      assertThat(listener.messageNotificationList.get(0).getSource().getPayload().getValue(),
                 equalTo(outboundEvent.getMessage().getPayload().getValue()));
    };

    endpoint.process(outboundEvent);

    assertEventSent();
    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertThat(listener.messageNotificationList, hasSize(2));
    assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_SEND_BEGIN));
    assertThat(listener.messageNotificationList.get(1).getAction(), is(MESSAGE_SEND_END));
    assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(InternalMessage.class));
    assertThat(listener.messageNotificationList.get(1).getSource(), instanceOf(InternalMessage.class));
    assertThat(listener.messageNotificationList.get(0).getSource().getPayload().getValue(),
               equalTo(outboundEvent.getMessage().getPayload().getValue()));
    assertThat(listener.messageNotificationList.get(1).getSource().getPayload().getValue(), is((Object) RESPONSE_MESSAGE));
  }

  @Test
  public void testDispatchNotification() throws Exception {
    final TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
    muleContext.registerListener(listener);

    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, ONE_WAY, null);
    final Event outboundEvent = createTestOutboundEvent();

    MyMessageDispatcherFactory.afterDispatch = input -> {
      try {
        assertTrue(listener.latchFirst.await(RECEIVE_TIMEOUT, MILLISECONDS));
      } catch (InterruptedException e) {
        fail(e.getMessage());
      }
      assertThat(listener.messageNotificationList, hasSize(1));
      assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_DISPATCH_BEGIN));
      assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(InternalMessage.class));
      assertThat(listener.messageNotificationList.get(0).getSource().getPayload().getValue(),
                 equalTo(outboundEvent.getMessage().getPayload().getValue()));
    };

    endpoint.process(outboundEvent);

    assertEventDispatched();
    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
    assertThat(listener.messageNotificationList, hasSize(2));
    assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_DISPATCH_BEGIN));
    assertThat(listener.messageNotificationList.get(1).getAction(), is(MESSAGE_DISPATCH_END));
    assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(InternalMessage.class));
    assertThat(listener.messageNotificationList.get(1).getSource(), instanceOf(InternalMessage.class));
    assertThat(listener.messageNotificationList.get(0).getSource().getPayload().getValue(),
               equalTo(outboundEvent.getMessage().getPayload().getValue()));
    assertThat(listener.messageNotificationList.get(1).getSource().getPayload().getValue(),
               equalTo(outboundEvent.getMessage().getPayload().getValue()));
  }

  @Test
  public void testTransformers() throws Exception {
    OutboundEndpoint endpoint =
        createOutboundEndpoint(null, null, new OutboundAppendTransformer(), new ResponseAppendTransformer(),
                               MessageExchangePattern.REQUEST_RESPONSE, null);

    testOutboundEvent = createTestOutboundEvent();

    Event result = endpoint.process(testOutboundEvent);

    assertNotNull(result);
    assertEquals(TEST_MESSAGE + OutboundAppendTransformer.APPEND_STRING,
                 MyMessageDispatcherFactory.dispatcher.sensedSendEvent.getMessageAsString(muleContext));
    assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING, result.getMessageAsString(muleContext));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testConnectorNotAvailableOnDynamicEndpoint() throws Exception {
    OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, REQUEST_RESPONSE, null);

    endpoint.getConnector();
  }

  @Test
  public void cachesResolvedStaticEndpoints() throws Exception {
    OutboundEndpoint prototypeEndpoint = mock(OutboundEndpoint.class);
    when(prototypeEndpoint.getMuleContext()).thenReturn(muleContext);

    EndpointBuilder staticEndpointBuilder = mock(EndpointBuilder.class);
    when(staticEndpointBuilder.buildOutboundEndpoint()).thenReturn(prototypeEndpoint);

    EndpointBuilder endpointBuilder = mock(EndpointBuilder.class);
    when(endpointBuilder.buildOutboundEndpoint()).thenReturn(prototypeEndpoint);
    when(endpointBuilder.clone()).thenReturn(staticEndpointBuilder);

    DynamicOutboundEndpoint dynamicOutboundEndpoint =
        new DynamicOutboundEndpoint(endpointBuilder,
                                    new DynamicURIBuilder(new URIBuilder("test://localhost:#[message.outboundProperties.port]",
                                                                         muleContext)));

    testOutboundEvent = createTestOutboundEvent();
    dynamicOutboundEndpoint.process(testOutboundEvent);
    dynamicOutboundEndpoint.process(testOutboundEvent);

    verify(endpointBuilder, times(1)).clone();
  }

  protected void assertMessageSentEqual(Event event) throws MuleException {
    assertEquals(TEST_MESSAGE, event.getMessageAsString(muleContext));
    assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
  }

  protected void assertEqualMessages(InternalMessage expect, InternalMessage actual) {
    assertThat(actual.getPayload().getValue(), equalTo(expect.getPayload().getValue()));
    assertEquals(expect.getPayload().getDataType(), actual.getPayload().getDataType());
  }

  private void assertEventDispatched() {
    Prober prober = new PollingProber();
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return MyMessageDispatcherFactory.dispatcher != null && MyMessageDispatcherFactory.dispatcher.dispatchedEvent;
      }

      @Override
      public String describeFailure() {
        return "Expected dispatcher was not called";
      }
    });

    assertNull(MyMessageDispatcherFactory.dispatcher.sensedSendEvent);
    assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent);
    assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent.getMessage());
  }

  private void assertEventSent() {
    Prober prober = new PollingProber();
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return MyMessageDispatcherFactory.dispatcher != null && MyMessageDispatcherFactory.dispatcher.sentEvent;
      }

      @Override
      public String describeFailure() {
        return "Expected dispatcher was not called";
      }
    });

    assertNull(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent);
    assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedSendEvent);
    assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedSendEvent.getMessage());
  }

  private void overrideDispatcherInServiceDescriptor() throws ServiceException {
    Properties props = new Properties();
    props.put(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, MyMessageDispatcherFactory.class.getName());
    DefaultTransportServiceDescriptor serviceDescriptor =
        (DefaultTransportServiceDescriptor) lookupServiceDescriptor(muleContext.getRegistry(), LegacyServiceType.TRANSPORT,
                                                                    "test", null);
    props.put(MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS, "ONE_WAY, REQUEST_RESPONSE");
    props.put(MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS, "ONE_WAY, REQUEST_RESPONSE");
    serviceDescriptor.setOverrides(props);
  }

  protected OutboundEndpoint createOutboundEndpoint(Filter filter, SecurityFilter securityFilter, Transformer in,
                                                    Transformer response, MessageExchangePattern exchangePattern,
                                                    TransactionConfig txConfig)
      throws Exception {

    return createTestOutboundEndpoint("test://localhost:#[message.outboundProperties.port]", filter, securityFilter, in, response,
                                      exchangePattern, txConfig);
  }

  private static class FakeMessageDispatcher extends TestMessageDispatcher {

    private final Closure afterSend;
    private final Closure afterDispatch;

    private Event sensedSendEvent;
    private Event sensedDispatchEvent;
    private boolean sentEvent;
    private boolean dispatchedEvent;

    public FakeMessageDispatcher(OutboundEndpoint endpoint, Closure afterSend, Closure afterDispatch) {
      super(endpoint);
      this.afterSend = afterSend;
      this.afterDispatch = afterDispatch;
    }

    @Override
    protected InternalMessage doSend(Event event) throws Exception {
      sensedSendEvent = event;
      sentEvent = true;
      afterSend.execute(event);
      return responseMessage;
    }

    @Override
    protected void doDispatch(Event event) throws Exception {
      sensedDispatchEvent = event;
      dispatchedEvent = true;
      afterDispatch.execute(event);
    }
  }

  public static class MyMessageDispatcherFactory extends TestMessageDispatcherFactory {

    static FakeMessageDispatcher dispatcher;

    static Closure afterSend;
    static Closure afterDispatch;

    @Override
    public synchronized MessageDispatcher create(OutboundEndpoint ep) throws MuleException {
      if (dispatcher != null) {
        throw new IllegalStateException("Dispatcher for this test was already created");
      }

      dispatcher = new FakeMessageDispatcher(ep, afterSend, afterDispatch);
      return dispatcher;
    }
  }
}
