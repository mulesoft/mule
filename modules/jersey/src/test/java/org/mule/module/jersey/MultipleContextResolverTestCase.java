/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MultipleContextResolverTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    private String configFile;

    public MultipleContextResolverTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                new Object[] {"multiple-context-resolver-config.xml"},
                new Object[] {"multiple-context-resolver-http-connector-config.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void supportsMultipleContextResolver() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);

        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/helloworld/sayHelloWithJson/World", TEST_MESSAGE, props);

        assertEquals((Integer) HttpConstants.SC_OK, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals(getHelloWorldMessage(), result.getPayloadAsString());
    }

    private String getHelloWorldMessage()
    {
        return "{\"message\":\"Hello World\",\"number\":0}";
    }
}
