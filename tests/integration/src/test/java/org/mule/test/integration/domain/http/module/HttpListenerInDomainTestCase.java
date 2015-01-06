/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http.module;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpListenerInDomainTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "domain/http/module/http-shared-listener-app.xml";
    }

    @Override
    protected String getDomainConfig()
    {
        return "domain/http/module/http-shared-listener-domain.xml";
    }

    @Test
    public void testAppConfiguration() throws Exception
    {
        String messageContent = "test";
        final HttpRequestOptions httpRequestOptions = HttpRequestOptionsBuilder.newOptions().method(POST.name()).build();
        MuleMessage response = muleContext.getClient().send(String.format("http://localhost:%s/test", port.getNumber()), new DefaultMuleMessage(messageContent, muleContext), httpRequestOptions);
        assertThat(response.getPayloadAsString(), is("testPayload"));
    }

}
