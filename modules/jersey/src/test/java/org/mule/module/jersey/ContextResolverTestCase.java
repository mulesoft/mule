/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.transport.http.HttpConstants.SC_OK;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

public class ContextResolverTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    private MuleClient client;
    private String urlMask;

    @Override
    protected String getConfigFile()
    {
        return "context-resolver-conf.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        client = muleContext.getClient();
        urlMask = String.format("http://localhost:%d", port.getNumber());
    }

    @Test
    public void contextResolver() throws Exception
    {
        assertResolved("/resolve");
    }

    @Test
    public void discoveredContextResolver() throws Exception
    {
        assertResolved("/discovered/resolve");
    }

    private void assertResolved(String url) throws Exception
    {
        url = getUrl(url);
        final HttpRequestOptions httpPostRequestOptions = newOptions().method(HttpConstants.Methods.GET.name()).disableStatusCodeValidation().build();
        final int COUNT = 3;
        final String MASK = "{\"message\":\"from contextResolver\",\"number\":%d}";

        for (int i = 0; i < COUNT; i++)
        {
            MuleMessage result = client.send(url, getTestMuleMessage(), httpPostRequestOptions);
            assertThat(result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0), is(SC_OK));
            assertThat(result.getPayloadAsString(), is(String.format(MASK, i)));
        }
    }

    private String getUrl(String path)
    {
        return urlMask + path;
    }
}
