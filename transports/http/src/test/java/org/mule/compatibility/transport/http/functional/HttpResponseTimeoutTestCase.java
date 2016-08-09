/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

/**
 * See MULE-4491 "Http outbound endpoint does not use responseTimeout attribute" See MULE-4743 "MuleClient.send() timeout is not
 * respected with http transport"
 */
public class HttpResponseTimeoutTestCase extends FunctionalTestCase {

  protected static String PAYLOAD = "Eugene";
  protected static int DEFAULT_RESPONSE_TIMEOUT = 2000;
  protected MuleClient muleClient;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-response-timeout-config-flow.xml";
  }

  protected String getPayload() {
    return PAYLOAD;
  }

  protected String getProcessedPayload() {
    return getPayload() + " processed";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    muleClient = muleContext.getClient();
  }

  @Test
  public void testDecreaseOutboundEndpointResponseTimeout() throws Exception {
    Date beforeCall = new Date();
    MuleMessage result = muleClient.send("vm://decreaseTimeoutRequest", getTestMessage());
    assertNotNull(result);
    assertNotNull(result.getExceptionPayload());
    assertEquals(DispatchException.class, result.getExceptionPayload().getException().getClass());

    // If everything is good the connection will timeout after 5s and throw an
    // exception. The original unprocessed message is returned in the response
    // message.
    Date afterCall = new Date();
    assertTrue((afterCall.getTime() - beforeCall.getTime()) < DEFAULT_RESPONSE_TIMEOUT);
  }

  private MuleMessage getTestMessage() {
    return MuleMessage.builder().payload(getPayload()).build();
  }

  @Test
  public void testIncreaseOutboundEndpointResponseTimeout() throws Exception {
    Date beforeCall = new Date();
    MuleMessage message = muleClient.send("vm://increaseTimeoutRequest", getPayload(), null);
    Date afterCall = new Date();

    // If everything is good the connection will not timeout and the processed
    // message will be returned as the response. There is no exception payload.
    assertNotNull(message);
    assertNull(message.getExceptionPayload());
    assertNotNull(getPayloadAsString(message));
    assertTrue((afterCall.getTime() - beforeCall.getTime()) > DEFAULT_RESPONSE_TIMEOUT);
  }

  @Test
  public void testDecreaseMuleClientSendResponseTimeout() throws Exception {
    Date beforeCall = new Date();
    Date afterCall;

    try {
      muleClient.send(getInDelayServiceAddress(), getTestMessage(), newOptions().responseTimeout(1000).build());
      fail("SocketTimeoutException expected");
    } catch (Exception e) {
      assertTrue(ExceptionUtils.getRootCause(e) instanceof TimeoutException);
    }

    // Exception should have been thrown after timeout specified which is
    // less than default.
    afterCall = new Date();
    assertTrue((afterCall.getTime() - beforeCall.getTime()) < DEFAULT_RESPONSE_TIMEOUT);
  }

  @Test
  public void testIncreaseMuleClientSendResponseTimeout() throws Exception {
    Date beforeCall = new Date();
    MuleMessage message = muleClient.send(getInDelayServiceAddress(), getTestMessage(), 3000);
    Date afterCall = new Date();

    // If everything is good the we'll have received a result after more than 10s
    assertNotNull(message);
    assertNull(message.getExceptionPayload());
    assertNotNull(getProcessedPayload(), getPayloadAsString(message));
    assertTrue((afterCall.getTime() - beforeCall.getTime()) > DEFAULT_RESPONSE_TIMEOUT);
  }

  protected String getInDelayServiceAddress() {
    return ((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("DelayService")).getMessageSource()).getAddress();
  }
}
