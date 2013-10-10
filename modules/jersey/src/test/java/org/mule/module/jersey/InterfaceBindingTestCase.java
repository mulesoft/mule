/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class InterfaceBindingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigResources()
    {
        return "interface-binding-config.xml";
    }

    @Test
    public void bindsComponentInterface() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map headers = new HashMap();
        headers.put("http.method", "GET");
        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/sayHello", "", headers);

        assertNull(result.getExceptionPayload());
        assertEquals("Hello World", result.getPayloadAsString());
    }
}
