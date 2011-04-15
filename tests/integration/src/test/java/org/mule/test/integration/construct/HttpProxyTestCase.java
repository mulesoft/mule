/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.construct;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

public class HttpProxyTestCase extends DynamicPortTestCase
{
    private MuleClient muleClient;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/http-proxy-config.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    public void testDirect() throws Exception
    {
        testDirectRequest(0);
    }

    public void testEndpointChildren() throws Exception
    {
        testDirectRequest(1);
    }

    public void testExceptionStrategy() throws Exception
    {
        testDirectRequest(2);
    }

    public void testTransforming() throws Exception
    {
        testRequest(3, "fooinbarout");
    }

    public void testInheritance() throws Exception
    {
        testRequest(4, "fooinbarout");
    }

    public void testDynamicAddress() throws Exception
    {
        testExtraHeadersRequest(5, Collections.singletonMap("proxyTarget", "bar-appender"));
    }

    // TODO (DDO) test path extensions, caching

    private void testDirectRequest(final int proxyId) throws Exception
    {
        testRequest(proxyId, "foobar");
    }

    private void testExtraHeadersRequest(final int proxyId, final Map<String, String> extraHeaders)
        throws Exception
    {
        testRequest(proxyId, "foobar", extraHeaders);
    }

    @SuppressWarnings("unchecked")
    private void testRequest(final int proxyId, final String expectedResult) throws Exception
    {
        testRequest(proxyId, expectedResult, Collections.EMPTY_MAP);
    }

    private void testRequest(final int proxyId,
                             final String expectedResult,
                             final Map<String, String> extraHeaders) throws Exception
    {
        final Map<String, String> headers = new HashMap<String, String>(Collections.singletonMap(
            "X-Custom-Header", "w00t"));
        headers.putAll(extraHeaders);

        final MuleMessage result = muleClient.send("http://localhost:" + getPorts().get(0) + "/http-proxy/"
                                                   + proxyId, "foo", headers, getTestTimeoutSecs() * 1000);
        assertEquals(expectedResult, result.getPayloadAsString());

        final int contentLength = getContentLength(result);
        assertEquals(expectedResult.length(), contentLength);

        assertEquals("w00tbaz", result.getInboundProperty("X-Custom-Header-Response"));
    }

    private int getContentLength(final MuleMessage result)
    {
        final Object messageProperty = result.getInboundProperty("Content-Length");
        return Integer.parseInt(messageProperty.toString());
    }
}
