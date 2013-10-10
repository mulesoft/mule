/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.xml.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.module.xml.transformer.wire.XStreamWireFormat;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.wire.AbstractMuleMessageWireFormatTestCase;

import java.util.Properties;

public class XStreamWireFormatTestCase extends AbstractMuleMessageWireFormatTestCase
{

    @Override
    protected WireFormat getWireFormat() throws Exception
    {
        return createObject(XStreamWireFormat.class);
    }

    @Override
    public void testGetDefaultInboundTransformer() throws Exception
    {
        assertEquals(XmlToObject.class, ((XStreamWireFormat) getWireFormat()).getInboundTransformer().getClass());
    }

    @Override
    public void testGetDefaultOutboundTransformer() throws Exception
    {
        assertEquals(ObjectToXml.class, ((XStreamWireFormat) getWireFormat()).getOutboundTransformer().getClass());
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

        Object outObject = readWrite(inOrange);

        // Test deserialized Fruit
        // TODO This wire-format wraps desrialized payloads in a message. See
        // MULE-3118
        // See test implementation in AbstractMuleMessageWireFormatTestCase.
        assertTrue(outObject instanceof MuleMessage);
        assertEquals("Walmart", ((Orange) ((MuleMessage) outObject).getPayload()).getBrand());
        assertEquals("val1", ((Orange) ((MuleMessage) outObject).getPayload()).getMapProperties().get("key1"));
    }

}
