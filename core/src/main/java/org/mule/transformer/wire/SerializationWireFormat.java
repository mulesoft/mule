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
        // TODO This is currently needed as a workaround for MULE-2881, this needs to
        // be removed is this is not the solution to MULE-2881
        SerializableToByteArray transformer = new SerializableToByteArray();
        transformer.setAcceptUMOMessage(true);
        setOutboundTransformer(transformer);
    }
}
