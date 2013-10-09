/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.hello;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class HelloTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

    @Test
    public void testHelloVM() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://greeter", "Ross", null);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("Ross"));
    }
}
