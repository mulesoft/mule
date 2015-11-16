/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpHeadersTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public HttpHeadersTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-headers-config-service.xml"},
            {ConfigVariant.FLOW, "http-headers-config-flow.xml"}
        });
    }

    @Test
    public void testJettyHeaders() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("clientEndpoint", getTestMuleMessage(null));

        String contentTypeProperty = result.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        assertNotNull(contentTypeProperty);
        assertEquals("application/x-download", contentTypeProperty);

        String contentDispositionProperty = result.getInboundProperty(HttpConstants.HEADER_CONTENT_DISPOSITION);
        assertNotNull(contentDispositionProperty);
        assertEquals("attachment; filename=foo.zip", contentDispositionProperty);
    }

    @Test
    public void handlesEmptyHeader() throws Exception
    {
        String url = String.format("http://localhost:%s", dynamicPort1.getNumber());
        GetMethod method = new GetMethod(url);
        method.addRequestHeader("Accept", null);
        int statusCode = new HttpClient().executeMethod(method);

        assertThat(statusCode, is(OK.getStatusCode()));
        assertThat(method.getResponseBodyAsString(), is(EMPTY));
    }

    public static class TestPayloadComponent implements Callable
    {
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            return eventContext.getMessage().getInboundProperty("accept");
        }
    }
}
