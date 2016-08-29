/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.junit.Assert.assertEquals;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.DynamicOutboundEndpoint;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EndpointURITestCase extends AbstractMuleContextEndpointTestCase {

  @Test
  public void testEndpoints() throws Exception {
    EndpointUri[] uris = {
        new EndpointUri("vm://#[message.inboundProperties.prop1]/#[message.inboundProperties.prop2]", "vm://apple/orange"),
        new EndpointUri("vm://bucket:somefiles?query=%7B%22filename%22%3A%22foo%22%7D"), new EndpointUri("http://localhost:1313"),
        new EndpointUri("http://localhost:1313?${foo}", "http://localhost:1313?$[foo]"),
        new EndpointUri("vm://#[message.inboundProperties.prop1]", "vm://apple"),};

    for (EndpointUri uri : uris) {
      if (!uri.isDynamic()) {
        InboundEndpoint ei = getEndpointFactory().getInboundEndpoint(uri.getUri());
        uri.checkResultUri(ei);
      }
      EndpointBuilder endpointBuilder = getEndpointFactory().getEndpointBuilder(uri.getUri());
      endpointBuilder.setExchangePattern(MessageExchangePattern.ONE_WAY);
      OutboundEndpoint oi = getEndpointFactory().getOutboundEndpoint(endpointBuilder);
      uri.checkResultUri(oi);
    }
  }

  private static class EndpointUri {

    private String uri;
    private boolean isDynamic;
    private String resultUri;
    private MuleMessage message;
    {
      Map<String, Serializable> inbound = new HashMap<>();
      inbound.put("prop1", "apple");
      inbound.put("prop2", "orange");
      inbound.put("prop3", "banana");
      message = MuleMessage.builder().payload("Hello, world").inboundProperties(inbound).build();
    }

    public EndpointUri(String uri, String resultUri) {
      this.uri = uri;
      this.resultUri = resultUri;
      this.isDynamic = true;
    }

    public EndpointUri(String uri) {
      this(uri, uri);
      this.isDynamic = false;
    }

    public String getUri() {
      return uri;
    }

    public boolean isDynamic() {
      return isDynamic;
    }

    public void checkResultUri(ImmutableEndpoint ep) throws Exception {
      String epUri;
      if (ep instanceof DynamicOutboundEndpoint) {
        Flow flow = getTestFlow();
        epUri = muleContext.getExpressionManager().parse(ep.getAddress(), MuleEvent
            .builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build(), flow, true);
      } else {
        epUri = ep.getAddress();
      }
      assertEquals(resultUri, epUri);
    }
  }
}
