/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EndpointURITestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testEndpoints() throws Exception
    {
        Client client = new Client(muleContext);

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
                InboundEndpoint ei = client.getInboundEndpoint(uri.getUri());
                uri.checkResultUri(ei);
            }
            if (!uri.getUri().startsWith("imap:"))
            {
                OutboundEndpoint oi = client.getOutboundEndpoint(uri.getUri());
                uri.checkResultUri(oi);
            }
        }
    }

    static class Client extends MuleClient
    {
        Client(MuleContext context) throws MuleException
        {
            super(context);
        }

        @Override
        public InboundEndpoint getInboundEndpoint(String uri) throws MuleException
        {
            return super.getInboundEndpoint(uri);
        }

        public OutboundEndpoint getOutboundEndpoint(String uri) throws MuleException
        {
            MessageExchangePattern mep = MessageExchangePattern.ONE_WAY;
            return getOutboundEndpoint(uri, mep, 0);
        }
    }

    static class EndpointUri
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

        EndpointUri(String uri, String resultUri)
        {
            this.uri = uri;
            this.resultUri = resultUri;
            this.isDynamic = true;
        }

        EndpointUri(String uri)
        {
            this.uri = uri;
            this.resultUri = uri;
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

        public String getResultUri()
        {
            return resultUri;
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
