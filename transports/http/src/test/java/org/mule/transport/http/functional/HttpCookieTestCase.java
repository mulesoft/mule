/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.Header;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpCookieTestCase extends AbstractMockHttpServerTestCase
{

    private CountDownLatch latch = new CountDownLatch(1);
    private boolean cookieFound = false;
    private List<String> cookieHeaders = new ArrayList<String>();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpCookieTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-cookie-test-service.xml"},
            {ConfigVariant.FLOW, "http-cookie-test-flow.xml"}});
    }

    @Override
    protected MockHttpServer getHttpServer(CountDownLatch serverStartLatch)
    {
        return new SimpleHttpServer(dynamicPort.getNumber(), serverStartLatch, latch);
    }

    @Test
    public void testCookies() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("COOKIE_HEADER", "MYCOOKIE");

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://vm-in", "foobar", properties);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(cookieFound);

        assertEquals(2, cookieHeaders.size());
        assertThereIsCookieWithThisContent("$Version=0; customCookie=yes", cookieHeaders);
        assertThereIsCookieWithThisContent("$Version=0; expressionCookie=MYCOOKIE", cookieHeaders);
    }

    private void assertThereIsCookieWithThisContent(String content, List<String> listOfRawCookies)
    {
        for (String rawCookie : listOfRawCookies)
        {
            if (rawCookie != null && rawCookie.contains(content))
            {
                return;
            }
        }
        fail("There should be a cookie with content '" + content + "': " + listOfRawCookies);

    }

    private class SimpleHttpServer extends SingleRequestMockHttpServer
    {

        public SimpleHttpServer(int listenPort, CountDownLatch startupLatch, CountDownLatch testCompleteLatch)
        {
            super(listenPort, startupLatch, testCompleteLatch, muleContext.getConfiguration().getDefaultEncoding());
        }

        @Override
        protected void processSingleRequest(HttpRequest httpRequest)
        {
            for (Header header : httpRequest.getHeaders())
            {
                if (header.getName().equals(HttpConstants.HEADER_COOKIE))
                {
                    cookieFound = true;
                    cookieHeaders.add(header.getValue());
                }
            }

        }
    }
}
