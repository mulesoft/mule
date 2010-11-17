/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.tck.DynamicPortTestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class HttpsFlowTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "https-flow-config.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    public void testSecureFlow() throws Exception
    {
        String url = String.format("https://localhost:%1d/?message=Hello", getPorts().get(0));

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        String result = method.getResponseBodyAsString();
        assertEquals("/?message=Hello received", result);
    }
}


