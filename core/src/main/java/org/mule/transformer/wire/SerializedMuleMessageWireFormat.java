/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.wire;

import org.mule.transformer.simple.ByteArrayToMuleMessage;
import org.mule.transformer.simple.MuleMessageToByteArray;

/**
 * Wire format using Java serialization to serialize MuleMessage objects accross the wire
 */
public class SerializedMuleMessageWireFormat extends TransformerPairWireFormat
{
    public SerializedMuleMessageWireFormat()
    {
        setInboundTransformer(new ByteArrayToMuleMessage());
        setOutboundTransformer(new MuleMessageToByteArray());
    }
}
