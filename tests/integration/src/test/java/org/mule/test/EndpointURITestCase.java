/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EndpointURITestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testEndpoints() throws Exception
    {
        EndpointUri[] uris =
        {
            new EndpointUri("vm://#[header:INBOUND:prop1]/#[header:INBOUND:prop2]", "vm://apple/orange"),
            new EndpointUri("imap://mule%40mule.net:secretpassword@gmail.com:143"),
            new EndpointUri("vm://bucket:somefiles?query=%7B%22filename%22%3A%22foo%22%7D"),
            new EndpointUri("http://localhost:1313"),
            new EndpointUri("http://localhost:1313?${foo}", "http://localhost:1313?$[foo]"),
            new EndpointUri("vm://#[header:INBOUND:prop1]", "vm://apple"),
        };

        for (EndpointUri uri : uris)
        {
            if (!uri.isDynamic())
            {
                InboundEndpoint ei = muleContext.getEndpointFactory().getInboundEndpoint(uri.getUri());
                uri.checkResultUri(ei);
            }
            if (!uri.getUri().startsWith("imap:"))
            {
                EndpointBuilder endpointBuilder =
                    muleContext.getEndpointFactory().getEndpointBuilder(uri.getUri());
                endpointBuilder.setExchangePattern(MessageExchangePattern.ONE_WAY);
                OutboundEndpoint oi =muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
                uri.checkResultUri(oi);
            }
        }
    }

    private static class EndpointUri
    {
        private String uri;
        private boolean isDynamic;
        private String resultUri;
        private MuleMessage message;
        {
            Map<String, Object> inbound = new HashMap<String, Object>();
            inbound.put("prop1", "apple");
            inbound.put("prop2", "orange");
            inbound.put("prop3", "banana");
            message = new DefaultMuleMessage("Hello, world", inbound, null, null, muleContext);
        }

        public EndpointUri(String uri, String resultUri)
        {
            this.uri = uri;
            this.resultUri = resultUri;
            this.isDynamic = true;
        }

        public EndpointUri(String uri)
        {
            this(uri, uri);
            this.isDynamic = false;
        }

        public String getUri()
        {
            return uri;
        }

        public boolean isDynamic()
        {
            return isDynamic;
        }

        public void checkResultUri(ImmutableEndpoint ep)
        {
            String epUri;
            if (ep instanceof DynamicOutboundEndpoint)
            {
                epUri = muleContext.getExpressionManager().parse(ep.getAddress(), message, true);
            }
            else
            {
                epUri = ep.getAddress();
            }
            assertEquals(resultUri, epUri);
        }
    }
}
