/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractMuleMessageWireFormatTestCase extends AbstractWireFormatTestCase
{

    @Override
    public void testWriteReadMessage() throws Exception
    {
        // Create message to send over wire
        Map<String, Serializable> messageProerties = new HashMap<>();
        messageProerties.put("key1", "val1");
        MuleMessage inMessage = new DefaultMuleMessage("testMessage", messageProerties);

        Object outMessage = readWrite(inMessage);

        // Test deserialized message
        // NOTE: As we are using SerializedMuleMessageWireFormat we get
        // MuleMessage rather than just the payload

        assertTrue(outMessage instanceof MuleMessage);
        assertEquals("testMessage", ((MuleMessage) outMessage).getPayload());
        assertEquals("val1", ((MuleMessage) outMessage).getOutboundProperty("key1"));
    }

    @Override
    public void testWriteReadPayload() throws Exception
    {
        // Create orange to send over the wire
        Properties messageProerties = new Properties();
        messageProerties.put("key1", "val1");
        Orange inOrange = new Orange();
        inOrange.setBrand("Walmart");
        inOrange.setMapProperties(messageProerties);

        try
        {
            readWrite(inOrange);
            fail("Expected exception: MuleMessageWireFormat does not support other types");
        }
        catch (Exception e)
        {
            // Expected
        }
    }

}
