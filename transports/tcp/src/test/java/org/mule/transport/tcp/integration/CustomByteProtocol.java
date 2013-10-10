/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import org.mule.transport.tcp.protocols.AbstractByteProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CustomByteProtocol extends AbstractByteProtocol
{

    /**
     * Create a CustomSerializationProtocol object.
     */
    public CustomByteProtocol()
    {
        super(false); // This protocol does not support streaming.
    }

    /**
     * Write the message's bytes to the socket, then terminate each message with '>>>'.
     */
    @Override
    protected void writeByteArray(OutputStream os, byte[] data) throws IOException
    {
        super.writeByteArray(os, data);
        os.write('>');
        os.write('>');
        os.write('>');
        os.flush();
    }

    /**
     * Read bytes until we see '>>>', which ends the message
     * */
    public Object read(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count = 0;
        byte read[] = new byte[1];

        while (true)
        {
            // if no bytes are currently avalable, safeRead() will wait until some arrive
            if (safeRead(is, read) < 0)
            {
                // We've reached EOF.  Return null, so that our caller will know there are no
                // remaining messages
                return null;
            }
            byte b = read[0];
            if (b == '>')
            {
                count++;
                if (count == 3)
                {
                    return baos.toByteArray();
                }
            }
            else
            {
                for (int i = 0; i < count; i++)
                {
                    baos.write('>');
                }
                count = 0;
                baos.write(b);
            }
        }
    }
}