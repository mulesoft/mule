/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpMultipleCookiesInEndpointTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpMultipleCookiesInEndpointTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-multiple-cookies-on-endpoint-test-service.xml"},
            {ConfigVariant.FLOW, "http-multiple-cookies-on-endpoint-test-flow.xml"}
        });
    }      
    
    @Test
    public void testThatThe2CookiesAreSentAndReceivedByTheComponent() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", "HELLO", null);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Both Cookies Found!", response.getPayloadAsString());
    }

}


