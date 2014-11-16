/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import java.util.Arrays;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpListenerMethodRoutingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty path = new SystemProperty("path", "path");
    private final String method;
    private final String expectedContent;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {{"GET", "GET"}, {"POST", "POST"}, {"OPTIONS", "OPTIONS-DELETE"}, {"DELETE", "OPTIONS-DELETE"}, {"PUT", "ALL"}});
    }

    public HttpListenerMethodRoutingTestCase(String method, String expectedContent)
    {
        this.method = method;
        this.expectedContent = expectedContent;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-listener-method-routing-config.xml";
    }

    @Test
    public void callWithMethod() throws Exception
    {
        final String url = String.format("http://localhost:%s/%s", listenPort.getNumber(), path.getValue());
        Request request = null;
        switch (method)
        {
            case "GET":
                request = Request.Get(url);
                break;
            case "POST":
                request = Request.Post(url);
                break;
            case "OPTIONS":
                request = Request.Options(url);
                break;
            case "DELETE":
                request = Request.Delete(url);
                break;
            case "PUT":
                request = Request.Put(url);
                break;
        }
        final Response response = request.connectTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(expectedContent));
    }

}
