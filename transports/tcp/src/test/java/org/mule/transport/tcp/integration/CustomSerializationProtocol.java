/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import org.mule.transport.tcp.protocols.DirectProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.SerializationUtils;

public class CustomSerializationProtocol extends DirectProtocol
{

    @Override
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

    @Override
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
