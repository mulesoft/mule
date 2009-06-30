/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.http.HttpConnector;

public class HttpStemTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "http-stem-test.xml";
    }

    public void testStemMatching() throws Exception
    {
        MuleClient client = new MuleClient();
        doTest(client, "http://localhost:60200/foo", "/foo", "/foo");
        doTest(client, "http://localhost:60200/foo/baz", "/foo", "/foo/baz");
        doTest(client, "http://localhost:60200/bar", "/bar", "/bar");
        doTest(client, "http://localhost:60200/bar/baz", "/bar", "/bar/baz");
    }
    
    protected void doTest(MuleClient client, final String url, final String contextPath, final String requestPath) throws Exception
    {
    	FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(contextPath);
        assertNotNull(testComponent);
         
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertEquals(requestPath, msg.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                assertEquals(requestPath, msg.getProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                assertEquals(contextPath, msg.getProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
            }
        };
     
        testComponent.setEventCallback(callback);
         
        MuleMessage result = client.send(url, "Hello World", null);
        assertEquals("Hello World Received", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

}


