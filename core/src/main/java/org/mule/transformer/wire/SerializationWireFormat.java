/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.wire;

import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.SerializableToByteArray;

/**
 * Wire format using Java serialization
 */
public class SerializationWireFormat extends TransformerPairWireFormat
{

    public SerializationWireFormat()
    {
        setInboundTransformer(new ByteArrayToSerializable());
        setOutboundTransformer(new SerializableToByteArray());
    }
    
}
