/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

public class JettyCustomExceptionStrategyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "jetty-custom-exception-strategy.xml";
    }

    @Test
    public void customExceptionStrategy() throws Exception
    {
        MuleClient client = muleContext.getClient();
        final HttpRequestOptions httpRequestOptions = newOptions().disableStatusCodeValidation().build();
        MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/test", getTestMuleMessage(), httpRequestOptions);
        assertNotNull(response);
        assertNotNull(response.getInboundProperty("CustomES"));
        assertEquals(response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY), 400);
    }
}
