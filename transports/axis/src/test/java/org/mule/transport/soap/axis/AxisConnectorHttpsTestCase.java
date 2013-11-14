/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.EndpointAware;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpsConnector;

import org.junit.Rule;
import org.junit.Test;

public class AxisConnectorHttpsTestCase extends FunctionalTestCase
{
    static ImmutableEndpoint endpoint;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "axis-https-connector-config.xml";
    }

    @Test
    public void testHttpsConnection() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InboundEndpoint inboundEndpoint = (InboundEndpoint) muleContext.getRegistry().lookupObject("inTestUMO");
        MuleMessage m = client.send(
            inboundEndpoint.getAddress() + "?method=echo", new DefaultMuleMessage("hello", muleContext));
        assertNotNull(m);

        // check that our https connector is being used
        assertNotNull(endpoint);
        assertTrue(endpoint.getConnector() instanceof HttpsConnector);
        assertTrue(endpoint.getConnector().getName().equals("myHttpsConnector"));
    }

    public static class AddConnectorMessageProperty implements MessageProcessor, EndpointAware
    {
        private ImmutableEndpoint myEndpoint;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            AxisConnectorHttpsTestCase.endpoint = myEndpoint;
            return event;
        }

        @Override
        public void setEndpoint(ImmutableEndpoint ep)
        {
            myEndpoint = ep;
        }
    }
}
