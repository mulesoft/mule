/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropertyScribblingMule893TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "issues/property-scribbling-mule-893-test.xml";
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
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
