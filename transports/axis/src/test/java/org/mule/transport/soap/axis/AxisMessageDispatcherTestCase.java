/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.Map;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AxisMessageDispatcherTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testNullParametersInCallAllowed() throws Exception
    {
        OutboundEndpoint ep = muleContext.getEndpointFactory().getOutboundEndpoint(
            "axis:http://www.muleumo.org/services/myService?method=myTestMethod");
        AxisMessageDispatcher dispatcher = new AxisMessageDispatcher(ep);
        dispatcher.service = new Service();
        MuleEvent event = getTestEvent("testPayload");
        // there should be no NullPointerException
        Call call = dispatcher.getCall(event, new Object[]{null});

        assertNotNull(call);

        MuleMessage msg = event.getMessage();
        assertNotNull(msg);
        final Map soapMethods = msg.getOutboundProperty(AxisConnector.SOAP_METHODS);
        assertEquals(1, soapMethods.size());
        final List values = (List)soapMethods.get("myTestMethod");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("value0;qname{:anyType:http://www.w3.org/2001/XMLSchema};in", values.get(0));
    }
}
