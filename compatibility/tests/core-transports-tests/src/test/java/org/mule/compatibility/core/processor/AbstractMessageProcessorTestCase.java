/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.processor;

import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.createEventUsingInboundEndpoint;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.createErrorMock;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.core.api.context.notification.EndpointMessageNotificationListener;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.security.EndpointSecurityFilter;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.endpoint.EndpointAware;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.mockito.stubbing.Answer;

public abstract class AbstractMessageProcessorTestCase extends AbstractMuleContextEndpointTestCase {

  protected static final String TEST_URI = "test://myTestUri";
  protected static String RESPONSE_MESSAGE = "response-message";
  protected static InternalMessage responseMessage;
  protected Answer<Event> echoEventAnswer = invocation -> (Event) invocation.getArguments()[0];

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    responseMessage = createTestResponseMuleMessage();
    muleContext.start();
  }

  @Override
  protected void configureMuleContext(MuleContextBuilder builder) {
    super.configureMuleContext(builder);

    // Configure EndpointMessageNotificationListener for notifications test
    ServerNotificationManager notificationManager = new ServerNotificationManager();
    notificationManager.addInterfaceToType(EndpointMessageNotificationListener.class,
                                           EndpointMessageNotification.class);
    notificationManager.addInterfaceToType(SecurityNotificationListener.class, SecurityNotification.class);

    builder.setNotificationManager(notificationManager);
  }

  protected InboundEndpoint createTestInboundEndpoint(Transformer transformer,
                                                      Transformer responseTransformer)
      throws EndpointException, InitialisationException {
    return createTestInboundEndpoint(null, null, transformer, responseTransformer, REQUEST_RESPONSE, null);
  }

  protected InboundEndpoint createTestInboundEndpoint(Filter filter,
                                                      SecurityFilter securityFilter,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig)
      throws InitialisationException, EndpointException {
    return createTestInboundEndpoint(filter, securityFilter, null, null, exchangePattern, txConfig);
  }

  protected InboundEndpoint createTestInboundEndpoint(Filter filter,
                                                      SecurityFilter securityFilter,
                                                      Transformer transformer,
                                                      Transformer responseTransformer,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig)
      throws EndpointException, InitialisationException {
    EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
    if (filter != null) {
      endpointBuilder.addMessageProcessor(new MessageFilter(filter));
    }
    if (securityFilter != null) {
      endpointBuilder.addMessageProcessor(new SecurityFilterMessageProcessor(securityFilter));
    }
    if (transformer != null) {
      endpointBuilder.setMessageProcessors(Collections.<Processor>singletonList(transformer));
    }
    if (responseTransformer != null) {
      endpointBuilder.setResponseMessageProcessors(Collections.<Processor>singletonList(responseTransformer));
    }
    endpointBuilder.setExchangePattern(exchangePattern);
    endpointBuilder.setTransactionConfig(txConfig);
    InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
    return endpoint;
  }

  protected Event createTestInboundEvent(InboundEndpoint endpoint) throws Exception {
    final InternalMessage message =
        InternalMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("prop1", "value1").build();
    return createEventUsingInboundEndpoint(eventBuilder(), message, endpoint);
  }

  protected OutboundEndpoint createTestOutboundEndpoint(Transformer transformer,
                                                        Transformer responseTransformer)
      throws EndpointException, InitialisationException {
    return createTestOutboundEndpoint(null, null, transformer, responseTransformer, REQUEST_RESPONSE, null);
  }

  protected OutboundEndpoint createTestOutboundEndpoint(Filter filter,
                                                        EndpointSecurityFilter securityFilter,
                                                        MessageExchangePattern exchangePattern,
                                                        TransactionConfig txConfig)
      throws InitialisationException, EndpointException {
    return createTestOutboundEndpoint(filter, securityFilter, null, null, exchangePattern,
                                      txConfig);
  }

  protected OutboundEndpoint createTestOutboundEndpoint(Filter filter,
                                                        EndpointSecurityFilter securityFilter,
                                                        Transformer transformer,
                                                        Transformer responseTransformer,
                                                        MessageExchangePattern exchangePattern,
                                                        TransactionConfig txConfig)
      throws EndpointException, InitialisationException {
    return createTestOutboundEndpoint("test://test", filter, securityFilter, transformer, responseTransformer, exchangePattern,
                                      txConfig);
  }

  protected OutboundEndpoint createTestOutboundEndpoint(String uri, Filter filter,
                                                        SecurityFilter securityFilter,
                                                        Transformer transformer,
                                                        Transformer responseTransformer,
                                                        MessageExchangePattern exchangePattern,
                                                        TransactionConfig txConfig)
      throws EndpointException, InitialisationException {
    EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri,
                                                                                muleContext);
    if (filter != null) {
      endpointBuilder.addMessageProcessor(new MessageFilter(filter));
    }
    if (securityFilter != null) {
      endpointBuilder.addMessageProcessor(new SecurityFilterMessageProcessor(securityFilter));
    }
    if (transformer != null) {
      endpointBuilder.setMessageProcessors(Collections.<Processor>singletonList(transformer));
    }
    if (responseTransformer != null) {
      endpointBuilder.setResponseMessageProcessors(Collections.<Processor>singletonList(responseTransformer));
    }
    endpointBuilder.setExchangePattern(exchangePattern);
    endpointBuilder.setTransactionConfig(txConfig);
    customizeEndpointBuilder(endpointBuilder);
    final OutboundEndpoint outboundEndpoint = endpointBuilder.buildOutboundEndpoint();
    outboundEndpoint.setFlowConstruct(new Flow("Flow for " + uri, muleContext));
    return outboundEndpoint;
  }

  protected void customizeEndpointBuilder(EndpointBuilder endpointBuilder) {
    // template method
  }

  protected Event createTestOutboundEvent() throws Exception {
    return createTestOutboundEvent(null);
  }

  protected Event createTestOutboundEvent(MessagingExceptionHandler exceptionListener) throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put("prop1", "value1");
    props.put("port", 12345);

    Flow flow = getTestFlow(muleContext);
    if (exceptionListener != null) {
      flow.setExceptionListener(exceptionListener);
    }
    final Event.Builder eventBuilder = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).flow(flow);
    final InternalMessage message = InternalMessage.builder().payload(TEST_MESSAGE).outboundProperties(props).build();
    return createEventUsingInboundEndpoint(eventBuilder, message, getTestInboundEndpoint(REQUEST_RESPONSE));
  }

  protected InternalMessage createTestResponseMuleMessage() {
    return InternalMessage.builder().payload(RESPONSE_MESSAGE).build();
  }

  public static class TestFilter implements Filter {

    public boolean accept;

    public TestFilter(boolean accept) {
      this.accept = accept;
    }

    @Override
    public boolean accept(InternalMessage message, Event.Builder builder) {
      return accept;
    }
  }

  public static class TestSecurityNotificationListener implements SecurityNotificationListener<SecurityNotification> {

    public SecurityNotification securityNotification;
    public Latch latch = new Latch();

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    public void onNotification(SecurityNotification notification) {
      securityNotification = notification;
      latch.countDown();
    }
  }

  public static class TestListener implements Processor {

    public Event sensedEvent;

    @Override
    public Event process(Event event) throws MuleException {
      sensedEvent = event;
      return event;
    }
  }

  public static class TestEndpointMessageNotificationListener
      implements EndpointMessageNotificationListener<EndpointMessageNotification> {

    public TestEndpointMessageNotificationListener() {
      latchFirst = new CountDownLatch(1);
      latch = new CountDownLatch(1);
    }

    public TestEndpointMessageNotificationListener(int numExpected) {
      latchFirst = new CountDownLatch(1);
      latch = new CountDownLatch(numExpected);
    }

    public EndpointMessageNotification messageNotification;
    public List<EndpointMessageNotification> messageNotificationList = new ArrayList<>();

    public CountDownLatch latchFirst;
    public CountDownLatch latch;

    @Override
    public void onNotification(EndpointMessageNotification notification) {
      messageNotification = notification;
      messageNotificationList.add(notification);
      if (latchFirst.getCount() > 0) {
        latchFirst.countDown();
      }
      latch.countDown();
    }
  }

  public static class ExceptionThrowingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new IllegalStateException();
    }
  }

  public static class TestExceptionListener implements MessagingExceptionHandler {

    public Exception sensedException;

    @Override
    public Event handleException(MessagingException exception, Event event) {
      sensedException = exception;
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).nullPayload()
          .exceptionPayload(new DefaultExceptionPayload(exception)).build()).error(createErrorMock(exception)).build();
    }

  }

  public class ObjectAwareProcessor implements Processor, EndpointAware, MuleContextAware {

    public MuleContext context;
    public ImmutableEndpoint endpoint;

    @Override
    public Event process(Event event) throws MuleException {
      return null;
    }

    @Override
    public void setEndpoint(ImmutableEndpoint endpoint) {
      this.endpoint = endpoint;
    }

    @Override
    public ImmutableEndpoint getEndpoint() {
      return endpoint;
    }

    @Override
    public void setMuleContext(MuleContext context) {
      this.context = context;
    }
  }

}
