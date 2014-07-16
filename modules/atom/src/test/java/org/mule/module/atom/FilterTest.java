/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FilterTest extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
