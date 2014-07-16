/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpProxyTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    public HttpProxyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/construct/http-proxy-config.xml"}

        });
    }

    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = muleContext.getClient();
    }

    @Test
    public void testDirect() throws Exception
    {
        testDirectRequest(0);
    }

    @Test
    public void testEndpointChildren() throws Exception
    {
        testDirectRequest(1);
    }

    @Test
    public void testExceptionStrategy() throws Exception
    {
        testDirectRequest(2);
    }

    @Test
    public void testTransforming() throws Exception
    {
        testRequest(3, "fooinbarout");
    }

    @Test
    public void testInheritance() throws Exception
    {
        testRequest(4, "fooinbarout");
    }

    @Test
    public void testDynamicAddress() throws Exception
    {
        testExtraHeadersRequest(5, Collections.singletonMap("proxyTarget", "bar-appender"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPathExtensions() throws Exception
    {
        testRequest(1, "/extension", "foobar", Collections.EMPTY_MAP);
        testRequest(1, "?name=value", "foobar", Collections.EMPTY_MAP);
        testRequest(1, "/other?name=value", "foobar", Collections.EMPTY_MAP);
    }

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
        testRequest(proxyId, StringUtils.EMPTY, expectedResult, extraHeaders);
    }

    private void testRequest(final int proxyId,
                             final String pathExtension,
                             final String expectedResult,
                             final Map<String, String> extraHeaders) throws Exception
    {
        Map<String, Object> headers = new HashMap<String, Object>(Collections.singletonMap(
            "X-Custom-Header", "w00t"));
        headers.putAll(extraHeaders);

        final MuleMessage result = muleClient.send("http://localhost:" + port1.getNumber() + "/http-proxy/"
                                                   + proxyId + pathExtension, "foo", headers,
            getTestTimeoutSecs() * 1000);
        assertEquals(expectedResult, result.getPayloadAsString());

        final int contentLength = getContentLength(result);
        assertEquals(expectedResult.length(), contentLength);

        assertEquals("w00tbaz", result.getInboundProperty("X-Custom-Header-Response"));
        assertEquals("/bar-appender" + pathExtension, result.getInboundProperty("X-Actual-Request-Path"));
    }

    private int getContentLength(final MuleMessage result)
    {
        final Object messageProperty = result.getInboundProperty("Content-Length");
        return Integer.parseInt(messageProperty.toString());
    }
}
