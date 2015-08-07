/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MultipleExceptionMapperTestCase extends org.mule.tck.junit4.FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    private String configFile;

    public MultipleExceptionMapperTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                new Object[] {"multiple-exception-mapper-config.xml"},
                new Object[] {"multiple-exception-mapper-http-connector-config.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void mapsToBeanBadRequestException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/helloworld/throwBadRequestException", getTestMuleMessage(), getHttpOptions());

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

        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/helloworld/throwException", getTestMuleMessage(), getHttpOptions());

        assertEquals((Integer) HttpConstants.SC_SERVICE_UNAVAILABLE, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
}
