/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
    {
        return "interface-binding-config.xml";
    }

    @Test
    public void bindsComponentInterface() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> headers = new HashMap<>();
        headers.put("http.method", "GET");
        MuleMessage result = client.send("http://localhost:" + port.getNumber() + "/sayHello", "", headers);

        assertNull(result.getExceptionPayload());
        assertEquals("Hello World", result.getPayloadAsString());
    }
}
