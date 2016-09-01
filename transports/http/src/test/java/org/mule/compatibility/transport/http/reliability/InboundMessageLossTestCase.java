/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.reliability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultSystemExceptionStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.tck.junit4.rule.DynamicPort;

/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of the HTTP transport, there is no way to restore the source message
 * so an exception is simply returned to the client.
 */
public class InboundMessageLossTestCase extends FunctionalTestCase {

  protected HttpClient httpClient = new HttpClient();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "reliability/inbound-message-loss-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    // Set SystemExceptionStrategy to redeliver messages (this can only be configured programatically for now)
    ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter("*"));
  }

  @Test
  public void testNoException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/noException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_OK, status);
    assertEquals("Here you go", request.getResponseBodyAsString());
  }

  @Test
  public void testTransformerException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/transformerException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
    assertTrue(request.getResponseBodyAsString().contains("Failure"));
  }

  @Test
  public void testHandledTransformerException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/handledTransformerException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_OK, status);
    assertTrue(request.getResponseBodyAsString().contains("Success"));
  }

  @Test
  public void testNotHandledTransformerException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/notHandledTransformerException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
    assertTrue(request.getResponseBodyAsString().contains("Bad news"));
  }

  @Test
  public void testRouterException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/routerException");
    int status = httpClient.executeMethod(request);
    assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
    assertTrue(request.getResponseBodyAsString().contains("Failure"));
  }

  @Test
  public void testComponentException() throws Exception {
    HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
    int status = httpClient.executeMethod(request);
    // Component exception occurs after the SEDA queue for an asynchronous request, but since
    // this request is synchronous, the failure propagates back to the client.
    assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
    assertTrue(request.getResponseBodyAsString().contains("exception"));
  }

  protected HttpMethodBase createRequest(String uri) {
    return new GetMethod(uri);
  }

  protected String getBaseUri() {
    return "http://localhost:" + dynamicPort.getNumber();
  }

  /**
   * Custom Exception Handler that handles an exception
   */
  public static class Handler extends AbstractMessagingExceptionStrategy {

    @Override
    public MuleEvent handleException(MessagingException ex, MuleEvent event) {
      doHandleException(ex, event);
      ex.setHandled(true);
      return MuleEvent.builder(event).message(MuleMessage.builder().payload("Success!").build()).build();
    }
  }

  /**
   * Custom Exception Handler that creates a different exception
   */
  public static class BadHandler extends AbstractMessagingExceptionStrategy {

    @Override
    public MuleEvent handleException(MessagingException ex, MuleEvent event) {
      doHandleException(ex, event);
      MessagingException exception = new MessagingException(event, new RuntimeException("Bad news!"));
      MuleMessage message = MuleMessage.builder()
          .nullPayload()
          .exceptionPayload(new DefaultExceptionPayload(exception))
          .build();
      return MuleEvent.builder(event).message(message).build();
    }
  }
}
