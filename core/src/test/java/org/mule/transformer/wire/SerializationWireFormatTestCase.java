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

import org.mule.api.transformer.wire.WireFormat;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.SerializableToByteArray;

public class SerializationWireFormatTestCase extends AbstractWireFormatTestCase
{

    protected WireFormat getWireFormat()
    {
        return new SerializationWireFormat();
    }

    public void testGetDefaultInboundTransformer()
    {
        SerializationWireFormat wireFormat = new SerializationWireFormat();
        assertEquals(ByteArrayToSerializable.class, wireFormat.getInboundTransformer().getClass());
    }

    public void testGetDefaultOutboundTransformer()
    {
        SerializationWireFormat wireFormat = new SerializationWireFormat();
        assertEquals(SerializableToByteArray.class, wireFormat.getOutboundTransformer().getClass());
    }

}
