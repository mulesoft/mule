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
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
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

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpCookieTestCase extends AbstractMockHttpServerTestCase
{

    private static final String EXPECTED_CUSTOM_COOKIE = "$Version=0; customCookie=yes";
    private static final String EXPECTED_EXPRESSION_COOKIE = "$Version=0; expressionCookie=MYCOOKIE";

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
    protected MockHttpServer getHttpServer()
    {
        return new SimpleHttpServer(dynamicPort.getNumber());
    }

    @Test
    public void sendsCookiesFromMapInPathWithoutEncodedCharacters() throws Exception
    {
        doRequest("testPath", getCookieMap());
        assertCookiesReceived(EXPECTED_CUSTOM_COOKIE, EXPECTED_EXPRESSION_COOKIE);
    }

    @Test
    public void sendsCookiesFromMapInPathWithEncodedCharacters() throws Exception
    {
        doRequest("testPath%25", getCookieMap());
        assertCookiesReceived(EXPECTED_CUSTOM_COOKIE, EXPECTED_EXPRESSION_COOKIE);
    }

    @Test
    public void sendsCookiesFromArrayInPathWithoutEncodedCharacters() throws Exception
    {
        doRequest("testPath", getCookieArray());
        assertCookiesReceived(EXPECTED_CUSTOM_COOKIE);
    }

    @Test
    public void sendsCookiesFromArrayInPathWithEncodedCharacters() throws Exception
    {
        doRequest("testPath%25", getCookieArray());
        assertCookiesReceived(EXPECTED_CUSTOM_COOKIE);
    }

    private void doRequest(String path, Object cookiesObject) throws Exception
    {
        Map<String, Object> outboundProperties = new HashMap<String, Object>();

        outboundProperties.put("COOKIE_HEADER", "MYCOOKIE");
        outboundProperties.put("PATH", path);
        outboundProperties.put("cookies", cookiesObject);

        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, outboundProperties, muleContext);

        client.dispatch("vm://vm-in", message);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(cookieFound);
    }

    private Map<String, String> getCookieMap()
    {
        Map<String, String> cookieMap = new HashMap<String, String>();
        cookieMap.put("customCookie", "yes");
        cookieMap.put("expressionCookie", "#[header:INBOUND:COOKIE_HEADER]");
        return cookieMap;
    }

    private Cookie[] getCookieArray()
    {
        Cookie[] cookieArray = new Cookie[1];
        cookieArray[0] = new Cookie("localhost", "customCookie", "yes");
        return cookieArray;
    }

    private void assertCookiesReceived(String... cookies)
    {
        assertEquals(cookies.length, cookieHeaders.size());
        for (String cookie : cookies)
        {
            assertThereIsCookieWithThisContent(cookie, cookieHeaders);
        }
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

        public SimpleHttpServer(int listenPort)
        {
            super(listenPort, muleContext.getConfiguration().getDefaultEncoding());
        }

        @Override
        protected void processSingleRequest(HttpRequest httpRequest) throws Exception
        {
            for (Header header : httpRequest.getHeaders())
            {
                if (header.getName().equals(HttpConstants.HEADER_COOKIE))
                {
                    cookieFound = true;
                    cookieHeaders.add(header.getValue());
                }
            }

            latch.countDown();
        }
    }
}
