/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpEndpointConstructTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-endpoint-construct-conf.xml";
    }

    @Test
    public void testHttpEndpointConstruct() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/testA", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());

    }


}
