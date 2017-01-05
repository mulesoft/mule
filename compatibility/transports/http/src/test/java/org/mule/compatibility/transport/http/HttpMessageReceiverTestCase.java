/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transport.AbstractMessageReceiverTestCase;
import org.mule.compatibility.transport.http.HttpMessageReceiver;
import org.mule.compatibility.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.util.CollectionUtils;

import org.junit.Before;
import org.junit.Test;

public class HttpMessageReceiverTestCase extends AbstractMessageReceiverTestCase {

  private static final String CONTEXT_PATH = "/resources";
  private static final String CLIENT_PATH = "/resources/client";
  private static final String CLIENT_NAME_PATH = "/resources/client/name";

  private HttpMessageReceiver httpMessageReceiver;

  @Override
  public MessageReceiver getMessageReceiver() throws Exception {
    Connector connector = endpoint.getConnector();
    return new HttpMessageReceiver(connector, mock(Flow.class), endpoint);
  }

  @Override
  public InboundEndpoint getEndpoint() throws Exception {
    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("http://localhost:6789", muleContext);
    endpointBuilder.setResponseTransformers(CollectionUtils.singletonList(new MuleMessageToHttpResponse()));
    endpoint = getEndpointFactory().getInboundEndpoint(endpointBuilder);
    return endpoint;
  }

  @Before
  public void setUp() throws Exception {
    httpMessageReceiver = (HttpMessageReceiver) getMessageReceiver();
  }


  @Test
  public void testProcessResourceRelativePath() {
    assertEquals("client", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CLIENT_PATH));
  }

  @Test
  public void testProcessRelativePathSameLevel() {
    assertEquals("", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CONTEXT_PATH));
  }

  @Test
  public void testProcessResourcePropertyRelativePath() {
    assertEquals("client/name", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CLIENT_NAME_PATH));
  }

  @Test
  public void messageSourceIsEndpointNotMessageReceiver() {
    MessageProcessContext messageContext = httpMessageReceiver.createMessageProcessContext();
    assertThat((InboundEndpoint) messageContext.getMessageSource(), is(httpMessageReceiver.getEndpoint()));
  }
}
