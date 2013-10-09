/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.wire;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractMuleMessageWireFormatTestCase extends AbstractWireFormatTestCase
{

    @Override
    public void testWriteReadMessage() throws Exception
    {
        // Create message to send over wire
        Map<String, Object> messageProerties = new HashMap<String, Object>();
        messageProerties.put("key1", "val1");
        MuleMessage inMessage = new DefaultMuleMessage("testMessage", messageProerties, muleContext);

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
