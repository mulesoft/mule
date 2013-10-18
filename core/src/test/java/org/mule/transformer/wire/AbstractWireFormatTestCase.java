/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.wire;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.simple.ObjectToString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractWireFormatTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testWriteReadMessage() throws Exception
    {
        // Create message to send over wire
        Map<String, Object> messageProerties = new HashMap<String, Object>();
        messageProerties.put("key1", "val1");
        MuleMessage inMessage = new DefaultMuleMessage("testMessage", messageProerties, muleContext);

        Object outMessage = readWrite(inMessage);

        // NOTE: Since we are not using SerializedMuleMessageWireFormat we only get
        // the payload back and not the MuleMessage.
        assertTrue(outMessage instanceof String);
        assertEquals("testMessage", outMessage);
    }

    @Test
    public void testWriteReadPayload() throws Exception
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

    protected Object readWrite(Object inObject) throws Exception
    {
        // Serialize
        WireFormat wireFormat = getWireFormat();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wireFormat.write(out, inObject, "UTF-8");

        // De-serialize
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Object outMessage = wireFormat.read(in);
        assertNotNull(outMessage);
        return outMessage;
    }

    @Test
    public void testSetInboundTransformer() throws Exception
    {
        TransformerPairWireFormat transPairWireFormat = (TransformerPairWireFormat) getWireFormat();
        transPairWireFormat.setInboundTransformer(new ObjectToString());
        assertTrue(transPairWireFormat.getInboundTransformer() instanceof ObjectToString);
    }

    @Test
    public void testSetOutboundTransformer() throws Exception
    {
        TransformerPairWireFormat transPairWireFormat = (TransformerPairWireFormat) getWireFormat();
        transPairWireFormat.setInboundTransformer(new ObjectToString());
        assertTrue(transPairWireFormat.getInboundTransformer() instanceof ObjectToString);
    }

    @Test
    public abstract void testGetDefaultInboundTransformer() throws Exception;

    @Test
    public abstract void testGetDefaultOutboundTransformer() throws Exception;

    protected abstract WireFormat getWireFormat() throws Exception;

}
