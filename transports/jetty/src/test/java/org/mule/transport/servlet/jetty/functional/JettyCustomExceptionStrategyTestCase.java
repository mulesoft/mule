/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty.functional;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

public class JettyCustomExceptionStrategyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    protected String getConfigResources()
    {
        return "jetty-custom-exception-strategy.xml";
    }

    @Test
    public void customExceptionStrategy() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/test", "test", null);
        assertNotNull(response);
        assertNotNull(response.getInboundProperty("CustomES"));
        assertEquals(response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY), "400");
    }
}
