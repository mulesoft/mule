/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.wire;

import static org.junit.Assert.assertEquals;

import org.mule.api.transformer.wire.WireFormat;
import org.mule.transformer.simple.ByteArrayToMuleMessage;
import org.mule.transformer.simple.MuleMessageToByteArray;

public class SerializedMuleMessageWireFormatTestCase extends AbstractMuleMessageWireFormatTestCase
{

    @Override
    protected WireFormat getWireFormat() throws Exception
    {
        return createObject(SerializedMuleMessageWireFormat.class);
    }

    @Override
    public void testGetDefaultInboundTransformer() throws Exception
    {
        SerializedMuleMessageWireFormat wireFormat = (SerializedMuleMessageWireFormat) getWireFormat();
        assertEquals(ByteArrayToMuleMessage.class, wireFormat.getInboundTransformer().getClass());
    }

    @Override
    public void testGetDefaultOutboundTransformer() throws Exception
    {
        SerializedMuleMessageWireFormat wireFormat = (SerializedMuleMessageWireFormat) getWireFormat();
        assertEquals(MuleMessageToByteArray.class, wireFormat.getOutboundTransformer().getClass());
    }

}
