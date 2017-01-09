/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractMuleClientTestCase extends AbstractMuleContextEndpointTestCase {

  private MuleClient muleClient;

  @Before
  public void before() throws MuleException {
    muleClient = createMuleClient();
  }

  protected MuleClient createMuleClient() throws MuleException {
    return new MuleClient(muleContext);
  }

  @After
  public void after() {
    muleClient.dispose();
  }

  @Test
  public void testInboundEndpointCache() throws MuleException {
    InboundEndpoint endpointa = muleClient.getInboundEndpoint("test://test1");
    InboundEndpoint endpointd = muleClient.getInboundEndpoint("test://test2");
    InboundEndpoint endpointc = muleClient.getInboundEndpoint("test://test1");
    assertThat(endpointc, equalTo(endpointa));
    assertThat(endpointd, not(sameInstance(endpointa)));
  }

  @Test
  public void testOutboundEndpointCache() throws MuleException {
    OutboundEndpoint endpointa = muleClient.getOutboundEndpoint("test://test1", REQUEST_RESPONSE, null);
    OutboundEndpoint endpointd = muleClient.getOutboundEndpoint("test://test2", REQUEST_RESPONSE, null);
    OutboundEndpoint endpointc = muleClient.getOutboundEndpoint("test://test1", REQUEST_RESPONSE, null);
    assertThat(endpointc, equalTo(endpointa));
    assertThat(endpointd, not(sameInstance(endpointa)));
  }


  public MuleClient getMuleClient() {
    return muleClient;
  }
}
