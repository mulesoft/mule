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
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;


public class JettyHttpStemTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jetty-http-stem-test.xml";
    }

    public void testStemMatchingHttp() throws Exception
    {
        MuleClient client = new MuleClient();
        doTest(client, "http://localhost:60200/foo");
        doTest(client, "http://localhost:60200/foo/bar");
    }

    public void testStemMatchingWthoutWildcards() throws Exception
    {
        MuleClient client = new MuleClient();
        doTest(client, "http://localhost:60202/foo");
        try
        {
            doTest(client, "http://localhost:60202/foo/bar");
            fail("endpoint not using a wildcard, the request should not be matched");
        }
        catch (Throwable e)
        {
            //Expected
        }
    }

    protected void doTest(MuleClient client, String url) throws Exception
    {
        MuleMessage result = client.send(url, "Hello", null);
        assertEquals("Hello World", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

}