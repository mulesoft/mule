/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.wire;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.Properties;

public abstract class AbstractMuleMessageWireFormatTestCase extends AbstractWireFormatTestCase
{

    public void testWriteReadMessage() throws Exception
    {
        // Create message to send over wire
        Properties messageProerties = new Properties();
        messageProerties.put("key1", "val1");
        MuleMessage inMessage = new DefaultMuleMessage("testMessage", messageProerties);

        Object outMessage = readWrite(inMessage);

        // Test deserialized message
        // NOTE: As we are using SerializedMuleMessageWireFormat we get
        // MuleMessage rather than just the payload

        assertTrue(outMessage instanceof MuleMessage);
        assertEquals("testMessage", ((MuleMessage) outMessage).getPayload());
        assertEquals("val1", ((MuleMessage) outMessage).getProperty("key1"));
    }

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
            Object outObject = readWrite(inOrange);
            fail("Expected exception: MuleMessageWireFormat does not support other types");
        }
        catch (Exception e)
        {
            // Expected
        }
    }

}
