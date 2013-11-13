/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
