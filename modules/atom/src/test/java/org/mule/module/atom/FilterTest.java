/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FilterTest extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "filter-conf.xml";
    }

    @Test
    public void testAcceptFilter() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage result;
//        = client.send("http://localhost:9002/bar/foo", "test", null);
//        assertEquals("test test", result.getPayloadAsString());

        result = client.send("http://localhost:9002/baz", "test", null);
        assertEquals("test received", result.getPayloadAsString());
    }

    @Test
    public void testUnAcceptFilter() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "HEAD");

        MuleMessage result = client.send("http://localhost:9002/baz", "test", props);
        //assertEquals(new Integer(0), result.getInboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, new Integer(-1)));
        assertEquals(new Integer(HttpConstants.SC_NOT_ACCEPTABLE), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, new Integer(-1)));

        result = client.send("http://localhost:9002/quo", "test", null);
        //assertEquals(new Integer(0), result.getInboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, new Integer(-1)));
        assertEquals(new Integer(HttpConstants.SC_NOT_ACCEPTABLE), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, new Integer(-1)));
    }

}
