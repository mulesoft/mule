/*
 * $Id: HttpMethodTestCase.java 7963 2007-08-21 08:53:15Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class HttpStemTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "http-stem-test.xml";
    }


    public void testStemMatchingHttp() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("http://localhost:60200/foo", "Hello World", null);
        assertEquals("Hello World", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        
        result = client.send("http://localhost:60200/foo/bar", "Hello World", null);
        assertEquals("Hello World", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
    
    public void testStemMatchingJetty() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("http://localhost:60201/foo", "Hello World", null);
        assertEquals("Hello World", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        
        result = client.send("http://localhost:60201/foo/bar", "Hello World", null);
        assertEquals("Hello World", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
}


