/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
