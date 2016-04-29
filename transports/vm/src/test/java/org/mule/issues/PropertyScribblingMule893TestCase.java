/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleProperties;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PropertyScribblingMule893TestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "issues/property-scribbling-mule-893-test.xml";
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "receive");

        client.dispatch("dispatch", "Message", properties);
        MuleMessage response = client.request("receive", 3000L);
        assertNotNull("Response is null", response);
        assertEquals("Message Received", response.getPayload());
    }

    @Test
    public void testManyMessages() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            testSingleMessage();
        }
    }
}
