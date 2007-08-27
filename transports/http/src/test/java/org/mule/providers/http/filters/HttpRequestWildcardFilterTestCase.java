/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.filters;

import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class HttpRequestWildcardFilterTestCase extends FunctionalTestCase
{

    public HttpRequestWildcardFilterTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "http-wildcard-filter-test.xml";
    }

    private static final String HTTP_ENDPOINT = "http://localhost:60201";
    private static final String REF_ENDPOINT = "http://localhost:60199";
    private static final String TEST_MESSAGE = "Hello=World";
    private static final String TEST_BAD_MESSAGE = "xyz";


    public void testReference() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(REF_ENDPOINT, TEST_MESSAGE, null);

        assertEquals(TEST_MESSAGE, result.getPayloadAsString());
    }

    public void testHttpPost() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(HTTP_ENDPOINT, TEST_MESSAGE, null);

        assertEquals(TEST_MESSAGE, result.getPayloadAsString());
    }

    public void testHttpGetNotFilter() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConstants.METHOD_GET, "true");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(HTTP_ENDPOINT, TEST_MESSAGE, props);

        assertEquals(TEST_MESSAGE, result.getPayloadAsString());
    }

    public void testHttpGetFilter() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConstants.METHOD_GET, "true");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(HTTP_ENDPOINT, TEST_BAD_MESSAGE, props);
        assertEquals(HttpConstants.SC_NOT_ACCEPTABLE, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertNotNull(result.getExceptionPayload());
    }

}
