/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.wire;

import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.SerializableToByteArray;

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
