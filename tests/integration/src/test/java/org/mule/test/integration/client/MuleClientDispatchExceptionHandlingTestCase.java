/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.core.api.Event.getCurrentEvent;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests to validate that MuleClient can be used from JavaComponent/MessageProcessor in order to dispatch an event to a sub-flow
 * and if the component/processor throws an exception afterwards the main-flow exception strategy handles it.
 */
public class MuleClientDispatchExceptionHandlingTestCase extends AbstractIntegrationTestCase {

  @ClassRule
  public static DynamicPort port = new DynamicPort("port");

  // These attributes need to be accessed from JavaComponent and MessageProcessor static classes therefore
  // they are declared as static
  private static Latch innerFlowLatch;
  private static Latch exceptionLatch;
  private static Event eventFromMainFlow;
  private static InternalMessage messageFromMainFlow;
  private static boolean eventPropagated;
  private static boolean isSameMessage;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/client/client-dispatch-catch-exception-flow.xml";
  }

  /**
   * Validates that a JavaComponent after doing a dispatch to a sub-flow using MuleClient throws an exception and the
   * on-error-continue defined in main-flow is called. It also validates that original event passed to JavaComponent is later
   * propagated to the JavaComponent defined in on-error-continue block.
   *
   * @throws Exception
   */
  @Test
  public void testCatchExceptionThrowFromJavaComponentToJavaComponent() throws Exception {
    doSendMessageToEndpoint("catchExceptionJavaComponentToJavaComponent");
  }

  @Test
  public void tesCatchExceptionThrowFromJavaComponentToMessageProcessor() throws Exception {
    doSendMessageToEndpoint("catchExceptionJavaComponentToMessageProcessor");
  }

  @Test
  public void testCatchExceptionThrowFromMessageProcessorToJavaComponent() throws Exception {
    doSendMessageToEndpoint("catchExceptionMessageProcessorToJavaComponent");
  }

  @Test
  public void tesCatchExceptionThrowFromMessageProcessorToMessageProcessor() throws Exception {
    doSendMessageToEndpoint("catchExceptionMessageProcessorToMessageProcessor");
  }

  @Test
  public void testCatchExceptionJavaComponentToJavaComponentRequestResponseInnerFlow() throws Exception {
    doSendMessageToEndpoint("catchExceptionJavaComponentToJavaComponentRequestResponseInnerFlow");
  }

  private void doSendMessageToEndpoint(String endpoint) throws Exception {
    innerFlowLatch = new Latch();
    exceptionLatch = new Latch();
    eventPropagated = true;
    isSameMessage = true;

    MuleClient client = muleContext.getClient();
    InternalMessage result = client.send(getUrl(endpoint), InternalMessage.of("Original Message")).getRight();

    boolean innerFlowCalled = innerFlowLatch.await(3, TimeUnit.SECONDS);
    assertThat(innerFlowCalled, is(true));
    boolean exceptionHandled = exceptionLatch.await(3, TimeUnit.SECONDS);
    assertThat(exceptionHandled, is(true));

    assertThat(isSameMessage, is(true));
    assertThat(eventPropagated, is(true));

    assertThat(result, notNullValue(InternalMessage.class));
  }

  private static String getUrl(String endpoint) {
    return String.format("http://localhost:%s/%s", port.getValue(), endpoint);
  }

  // Just a simple JavaComponent used in on-error-continue block
  // in order to check that RequestContext has the correct event and message references
  public static class AssertEventComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      // Validates that if another Component access to the getCurrentEvent() the one returned
      // is the correct, in this case it should be the same that it was set before doing the
      // eventContext.dispatchEvent() on main-flow java component where the exception happened right after
      // that invocatioeventPropagated = getCurrentEvent().equals(eventFromMainFlow);
      // Checking if message is still the same on on-error-continue
      isSameMessage =
          Objects.equals(getCurrentEvent().getMessage().getPayload().getValue(), messageFromMainFlow.getPayload().getValue());
      return eventContext.getMessage();
    }
  }

  public static class AssertEventProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      // Validates that if another Component access to the getCurrentEvent() the one returned
      // is the correct, in this case it should be the same that it was set before doing the
      // eventContext.dispatchEvent() on main-flow java component where the exception happened right after
      // that invocatioeventPropagated = getCurrentEvent().equals(eventFromMainFlow);
      // Checking if message is still the same on on-error-continue
      isSameMessage =
          Objects.equals(getCurrentEvent().getMessage().getPayload().getValue(), messageFromMainFlow.getPayload().getValue());
      return event;
    }
  }

  public static class DispatchInnerFlowThrowExceptionJavaComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      eventFromMainFlow = getCurrentEvent();
      messageFromMainFlow = eventFromMainFlow.getMessage();

      muleContext.getClient().dispatch(getUrl("innertest"), InternalMessage.builder().payload("payload").build());

      throw new Exception("expected exception!");
    }
  }

  public static class SendInnerFlowThrowExceptionJavaComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      eventFromMainFlow = getCurrentEvent();
      messageFromMainFlow = eventFromMainFlow.getMessage();

      muleContext.getClient().send(getUrl("innerrequestresponsetest"), InternalMessage.builder().payload("payload").build());

      throw new Exception("expected exception!");
    }
  }

  public static class DispatchInnerFlowThrowExceptionMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      eventFromMainFlow = getCurrentEvent();
      messageFromMainFlow = eventFromMainFlow.getMessage();

      muleContext.getClient().dispatch(getUrl("innertest"), InternalMessage.builder().payload("payload").build());

      throw new DefaultMuleException("expected exception!");
    }
  }

  public static class ExecutionCountDownProcessor implements Processor {

    @Override
    public synchronized Event process(Event event) throws MuleException {
      exceptionLatch.countDown();
      return event;
    }
  }

  public static class InnerFlowCountDownProcessor implements Processor {

    @Override
    public synchronized Event process(Event event) throws MuleException {
      innerFlowLatch.countDown();
      return event;
    }
  }
}
