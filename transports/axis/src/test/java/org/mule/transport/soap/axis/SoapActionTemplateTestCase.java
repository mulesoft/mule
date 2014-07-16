/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.xml.namespace.QName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SoapActionTemplateTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHostInfoReplace() throws Exception
    {
        OutboundEndpoint ep = muleContext.getEndpointFactory().getOutboundEndpoint(
            "axis:http://mycompany.com:8080/services/myService?method=foo");
        
        AxisMessageDispatcher dispatcher = new AxisMessageDispatcher(ep);
        MuleEvent event = getTestEvent("test,");
        String result = dispatcher.parseSoapAction("#[hostInfo]/#[method]", new QName("foo"), event);

        assertEquals("http://mycompany.com:8080/foo", result);
    }

    @Test
    public void testHostReplace() throws Exception
    {
        OutboundEndpoint ep = muleContext.getEndpointFactory().getOutboundEndpoint(
            "axis:http://mycompany.com:8080/services/myService?method=foo");
        AxisMessageDispatcher dispatcher = new AxisMessageDispatcher(ep);
        MuleEvent event = getTestEvent("test,");
        String name = event.getFlowConstruct().getName();
        String result = dispatcher.parseSoapAction("#[scheme]://#[host]:#[port]/#[serviceName]/#[method]",
            new QName("foo"), event);

        assertEquals("http://mycompany.com:8080/" + name + "/foo", result);
    }
}
