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

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

// FIXME (DDO) use DynamicPortTestCase
public class HttpProxyTestCase extends FunctionalTestCase
{
    private MuleClient muleClient;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/http-proxy-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
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
        // FIXME (DDO) should be "fooinbarout": why isn't the transformer on outbound http executing?
        testRequest(3, "foobarout");
    }

    // TODO (DDO) test inheritance, dynamic endpoints, caching

    private void testDirectRequest(final int proxyId) throws Exception
    {
        testRequest(proxyId, "foobar");
    }

    private void testRequest(final int proxyId, final String expectedResult) throws Exception
    {
        final MuleMessage result = muleClient.send("http://localhost:8090/bar-appender/" + proxyId, "foo",
            Collections.singletonMap("X-Custom-Header", "w00t"), getTestTimeoutSecs() * 1000);
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
