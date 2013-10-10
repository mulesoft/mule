/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpsConnector;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AxisConnectorHttpsTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "axis-https-connector-config.xml";
    }

    @Test
    public void testHttpsConnection() throws Exception{
        MuleClient client = new MuleClient(muleContext);
        MuleMessage m = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inTestUMO")).getAddress() + "?method=echo",new DefaultMuleMessage("hello", muleContext));
        assertNotNull(m);

        // check that our https connector is being used
        MuleEvent event = RequestContext.getEvent();
        assertTrue (event.getEndpoint().getConnector() instanceof HttpsConnector);
        assertTrue(event.getEndpoint().getConnector().getName().equals("myHttpsConnector"));
    }

}


