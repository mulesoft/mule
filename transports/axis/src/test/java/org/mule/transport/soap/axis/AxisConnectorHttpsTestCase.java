/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.EndpointAware;
import org.mule.module.client.MuleClient;
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
    protected String getConfigResources()
    {
        return "axis-https-connector-config.xml";
    }

    @Test
    public void testHttpsConnection() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage m = client.send(
            ((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inTestUMO")).getAddress()
                            + "?method=echo", new DefaultMuleMessage("hello", muleContext));
        assertNotNull(m);

        // check that our https connector is being used
        assertNotNull(endpoint);
        assertTrue(endpoint.getConnector() instanceof HttpsConnector);
        assertTrue(endpoint.getConnector().getName().equals("myHttpsConnector"));
    }

    public static class AddConnectorMessageProperty implements MessageProcessor, EndpointAware
    {
        private ImmutableEndpoint endpoint;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            AxisConnectorHttpsTestCase.endpoint = endpoint;
            return event;
        }

        @Override
        public void setEndpoint(ImmutableEndpoint ep)
        {
            endpoint = ep;
        }
    }

}
