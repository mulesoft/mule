/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.functional.HttpFunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

public class JettyHttpEncodingFunctionalTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (Rødgrød), 57 = \u06f7\u06f5 in Arabic";

    protected String getConfigResources()
    {
        return "jetty-http-encoding-test.xml";
    }

    public void testSendWithProperResponseContentType() throws Exception
    {
        MuleClient client = new MuleClient();
        Map messageProperties = new HashMap();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());
        MuleMessage reply = client.send("clientEndpoint", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/baz;charset=UTF-16BE", reply.getProperty(HttpConstants.HEADER_CONTENT_TYPE).toString());
        assertEquals("UTF-16BE", reply.getEncoding());
        assertEquals(TEST_MESSAGE + " Received", reply.getPayloadAsString());
    }
    
    /**
     * MULE-4031 - ensure the content type isn't copied
     */
    public void testSendWithInvalidResponseContentType() throws Exception
    {
        MuleClient client = new MuleClient();
        Map messageProperties = new HashMap();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());
        MuleMessage reply = client.send("clientEndpoint2", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/plain", reply.getProperty(HttpConstants.HEADER_CONTENT_TYPE).toString());
        assertEquals("UTF-8", reply.getEncoding());
    }

    protected String getSendEncoding()
    {
        return "text/plain;charset=UTF-8";
    }
}