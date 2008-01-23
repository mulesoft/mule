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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.simple.ObjectToString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

public abstract class AbstractWireFormatTestCase extends AbstractMuleTestCase
{

    public void testWriteReadPayload() throws MuleException
    {
        // Create orange to send over the wire
        Properties messageProerties = new Properties();
        messageProerties.put("key1", "val1");
        Orange inOrange = new Orange();
        inOrange.setBrand("Walmart");
        inOrange.setMapProperties(messageProerties);

        Object outObject = readWrite(inOrange);

        // Test deserialized Fruit
        assertTrue(outObject instanceof Orange);
        assertEquals("Walmart", ((Orange) outObject).getBrand());
        assertEquals("val1", ((Orange) outObject).getMapProperties().get("key1"));
    }

    public void testWriteReadMessage() throws MuleException
    {
        // Create message to send over wire
        Properties messageProerties = new Properties();
        messageProerties.put("key1", "val1");
        MuleMessage inMessage = new DefaultMuleMessage("testMessage", messageProerties);

        Object outMessage = readWrite(inMessage);

        // Test deserialized message
        assertTrue(outMessage instanceof MuleMessage);
        assertEquals("testMessage", ((MuleMessage) outMessage).getPayload());
        assertEquals("val1", ((MuleMessage) outMessage).getProperty("key1"));
    }

    private Object readWrite(Object inObject) throws MuleException
    {
        // Serialize
        WireFormat wireFormat = new SerializationWireFormat();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wireFormat.write(out, inObject, "UTF-8");

        // De-serialize
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Object outMessage = wireFormat.read(in);
        assertNotNull(outMessage);
        return outMessage;
    }

    public void testSetInboundTransformer() throws Exception
    {
        TransformerPairWireFormat transPairWireFormat = (TransformerPairWireFormat) getWireFormat();
        transPairWireFormat.setInboundTransformer(new ObjectToString());
        assertTrue(transPairWireFormat.getInboundTransformer() instanceof ObjectToString);
    }

    public void testSetOutboundTransformer() throws Exception
    {
        TransformerPairWireFormat transPairWireFormat = (TransformerPairWireFormat) getWireFormat();
        transPairWireFormat.setInboundTransformer(new ObjectToString());
        assertTrue(transPairWireFormat.getInboundTransformer() instanceof ObjectToString);
    }

    public abstract void testGetDefaultInboundTransformer() throws Exception;

    public abstract void testGetDefaultOutboundTransformer() throws Exception;

    protected abstract WireFormat getWireFormat() throws Exception;

}
