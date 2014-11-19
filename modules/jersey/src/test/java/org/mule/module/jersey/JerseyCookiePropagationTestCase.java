/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mule.DefaultMuleMessage;

@RunWith(Parameterized.class)
public class JerseyCookiePropagationTestCase extends FunctionalTestCase
{
    private static String TEST_COOKIE_NAME = "testCookie";
    private static String TEST_COOKIE_VALUE = "somevalue";
    private final boolean useOldTransport;

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                new Object[] {true, "jersey-cookie-config-flow.xml"},
                new Object[] {false, "jersey-cookie-http-connector-config-flow.xml"}
        });
    }

    private String configFile;

    public JerseyCookiePropagationTestCase(boolean useOldTransport, String configFile)
    {
        this.useOldTransport = useOldTransport;
        this.configFile = configFile;
    }


    @Override
    public String getConfigFile()
    {
        return configFile;
    }

    @Ignore("See MULE-8070")
    @Test
    public void testJerseyCookiePropagation() throws Exception
    {
        HelloWorldCookieResource jerseyResource = muleContext.getRegistry().get("jerseyComponent");

        Map<String, Object> props = new HashMap<String, Object>();
        Cookie originalCookie = new Cookie(null, TEST_COOKIE_NAME, TEST_COOKIE_VALUE);

        Cookie[] cookiesObject = new Cookie[] { originalCookie };
        props.put(HttpConnector.HTTP_COOKIES_PROPERTY, cookiesObject);

        MuleMessage result;
        if (useOldTransport)
        {

            props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
            result = muleContext.getClient().send(
                    "http://localhost:" + httpPort.getNumber() + "/helloworld", getTestMessage(props));
        }
        else
        {
            final HttpRequestOptions httpRequestOptions = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).build();
            result = muleContext.getClient().send(
                    "http://localhost:" + httpPort.getNumber() + "/helloworld", getTestMessage(props), httpRequestOptions);
        }

        assertThat(HttpStatus.SC_OK, is(equalTo(result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0))));

        Map<String, javax.ws.rs.core.Cookie> cookies = jerseyResource.getCookies();
        javax.ws.rs.core.Cookie jerseyCookie = jerseyResource.getCookies().get(TEST_COOKIE_NAME);
        
        assertThat(1, is(equalTo(cookies.size())));
        assertThat(originalCookie.getName(), is(equalTo(jerseyCookie.getName())));
        assertThat(originalCookie.getValue(), is(equalTo(jerseyCookie.getValue())));
        assertThat(originalCookie.getValue(), is(equalTo(jerseyResource.getTestCookie())));
    }

    private DefaultMuleMessage getTestMessage(Map<String, Object> props)
    {
        return new DefaultMuleMessage("", props, muleContext);
    }

}
