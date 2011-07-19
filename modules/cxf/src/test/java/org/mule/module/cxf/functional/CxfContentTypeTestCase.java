/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CxfContentTypeTestCase extends FunctionalTestCase
{
    private static final String requestPayload =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "cxf-echo-service-conf.xml";
    }

    @Test
    public void testCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/hello", request);
        String contentType = received.getInboundProperty("content-type");
        assertNotNull(contentType);
        assertTrue(contentType.contains("charset"));
    }

    @Test
    public void testCxfClient() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://helloClient", request);
        String contentType = received.getInboundProperty("contentType");
        assertNotNull(contentType);
        assertTrue(contentType.contains("charset"));
    }

}
