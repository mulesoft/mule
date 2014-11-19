/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import org.junit.Rule;
import org.junit.Test;

public class MultipleExceptionMapperTestCase extends org.mule.tck.junit4.FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "multiple-exception-mapper-config.xml";
    }

    @Test
    public void mapsToBeanBadRequestException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/helloworld/throwBadRequestException", new DefaultMuleMessage(TEST_MESSAGE, muleContext), getHttpOptions());

        assertEquals((Integer) HttpConstants.SC_BAD_REQUEST, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

    private HttpRequestOptions getHttpOptions()
    {
        return newOptions().disableStatusCodeValidation().build();
    }


    @Test
    public void mapsToHelloWorldException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/helloworld/throwException", new DefaultMuleMessage(TEST_MESSAGE, muleContext), getHttpOptions());

        assertEquals((Integer) HttpConstants.SC_SERVICE_UNAVAILABLE, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
}
