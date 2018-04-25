/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.X_CORRELATION_ID;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestCorrelationIdTestCase extends AbstractHttpRequestTestCase
{

  private static final String MESSAGE_CORRELATION_ID = "messageCorrelationId";

  @Rule
  public DynamicPort serverPort = new DynamicPort("serverPort");

  @Override
  protected String getConfigFile()
  {
    return "http-request-correlation-id-config.xml";
  }

  @Test
  public void sendsMessageCorrelationId() throws Exception
  {
    sendsMuleCorrelationIdWithValue("default", MESSAGE_CORRELATION_ID);
  }

  @Test
  public void propertyOverridesMessageCorrelationId() throws Exception
  {
    sendsMuleCorrelationIdWithValue("property", "propertyCorrelationId");
  }

  @Test
  public void headerOverridesMessageCorrelationId() throws Exception
  {
    sendsMuleCorrelationIdWithValue("header", "headerCorrelationId");
  }

  @Test
  public void usesListenerMuleCorrelationId() throws Exception
  {
    propagatesMuleCorrelationIdFromListenerHeader(MULE_CORRELATION_ID_PROPERTY);
  }

  @Test
  public void usesListenerXCorrelationId() throws Exception
  {
    propagatesMuleCorrelationIdFromListenerHeader(X_CORRELATION_ID);
  }

  private void sendsMuleCorrelationIdWithValue(String flow, String expectedId) throws Exception
  {
    MuleEvent event = getTestEvent(TEST_PAYLOAD);
    event.getMessage().setCorrelationId(MESSAGE_CORRELATION_ID);
    runFlow(flow, event);
    Collection<String> correlationIdHeaders = headers.get(MULE_CORRELATION_ID_PROPERTY);
    assertThat(correlationIdHeaders, hasSize(1));
    assertThat(correlationIdHeaders.iterator().next(), is(expectedId));
  }

  private void propagatesMuleCorrelationIdFromListenerHeader(String listenerCorrelationIdHeader) throws IOException
  {
    String listenerCorrelationId = "listenerCorrelationId";
    Request.Get(format("http://localhost:%s/", serverPort.getValue()))
      .addHeader(listenerCorrelationIdHeader, listenerCorrelationId)
      .execute();
    assertThat(getFirstReceivedHeader(MULE_CORRELATION_ID_PROPERTY), is(listenerCorrelationId));
  }

}
