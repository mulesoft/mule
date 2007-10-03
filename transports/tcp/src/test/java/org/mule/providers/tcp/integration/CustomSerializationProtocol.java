/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import org.mule.providers.tcp.protocols.DirectProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.SerializationUtils;

public class CustomSerializationProtocol extends DirectProtocol
{

    // @Override
    public void write(OutputStream os, Object data) throws IOException
    {
        if (data instanceof NonSerializableMessageObject)
        {
            NonSerializableMessageObject in = (NonSerializableMessageObject)data;

            // do serialization... will use normal Serialization to simplify code...
            MessageObject serializableObject = new MessageObject(in.i, in.s, in.b);

            write(os, SerializationUtils.serialize(serializableObject));
        }
        else
        {
            super.write(os, data);
        }
    }

    // @Override
    public Object read(InputStream is) throws IOException
    {
        byte[] tmp = (byte[]) super.read(is);

        if (tmp == null)
        {
            return null;
        }
        else
        {
            MessageObject serializableObject = (MessageObject)SerializationUtils.deserialize(tmp);
            return new NonSerializableMessageObject(serializableObject.i, serializableObject.s,
                serializableObject.b);
        }
    }

}
