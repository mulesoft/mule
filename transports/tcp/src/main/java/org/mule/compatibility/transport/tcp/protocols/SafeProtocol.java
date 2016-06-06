/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.protocols;

import org.mule.compatibility.transport.tcp.TcpProtocol;
import org.mule.runtime.core.ResponseOutputStream;
import org.mule.runtime.core.api.serialization.DefaultObjectSerializer;
import org.mule.runtime.core.api.serialization.ObjectSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.inject.Inject;

/**
 * This precedes every message with a cookie.
 * It should probably not be used in production.
 * We use ths protocol as the default because previously people tended to use DefaultProtocol without considering packet fragmentation etc.
 * You should probably change to LengthProtocol.
 * Remember - both sender and receiver must use the same protocol.
 */
public class SafeProtocol implements TcpProtocol
{

    public static final String COOKIE = "You are using SafeProtocol";
    private TcpProtocol delegate = new LengthProtocol();
    private TcpProtocol cookieProtocol = new LengthProtocol(COOKIE.length());

    @Override
    public Object read(InputStream is) throws IOException
    {
        if (assertSiblingSafe(is))
        {
            Object result = delegate.read(is);
            if (null == result)
            {
                // EOF after cookie but before data
                helpUser();
            }
            return result;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void write(OutputStream os, Object data) throws IOException
    {
        assureSibling(os);
        delegate.write(os, data);
    }

    @Override
    public ResponseOutputStream createResponse(Socket socket) throws IOException
    {
        return new ResponseOutputStream(socket, new ProtocolStream(this, false, socket.getOutputStream()));
    }

    private void assureSibling(OutputStream os) throws IOException
    {
        cookieProtocol.write(os, COOKIE);
    }

    /**
     * @param is Stream to read data from
     * @return true if further data are available; false if EOF
     * @throws IOException
     */
    private boolean assertSiblingSafe(InputStream is) throws IOException
    {
        Object cookie = null;
        try
        {
            cookie = cookieProtocol.read(is);
        }
        catch (Exception e)
        {
            helpUser(e);
        }
        if (null != cookie)
        {
            if (!(cookie instanceof byte[]
                  && ((byte[]) cookie).length == COOKIE.length()
                  && COOKIE.equals(new String((byte[]) cookie))))
            {
                helpUser();
            }
            else
            {
                return true;
            }
        }
        return false; // eof
    }

    private void helpUser() throws IOException
    {
        throw new IOException("You are not using a consistent protocol on your TCP transport. "
                              + "Please read the documentation for the TCP transport, "
                              + "paying particular attention to the protocol parameter.");
    }

    private void helpUser(Exception e) throws IOException
    {
        throw (IOException) new IOException("An error occurred while verifying your connection.  "
                                            + "You may not be using a consistent protocol on your TCP transport. "
                                            + "Please read the documentation for the TCP transport, "
                                            + "paying particular attention to the protocol parameter.").initCause(e);
    }

    public void setMaxMessageLength(int maxMessageLength)
    {
        delegate = new LengthProtocol(maxMessageLength);
    }

    @Inject
    @DefaultObjectSerializer
    public void setObjectSerializer(ObjectSerializer objectSerializer)
    {
        propagateObjectSerializerIfNecessary(delegate, objectSerializer);
        propagateObjectSerializerIfNecessary(cookieProtocol, objectSerializer);
    }

    private void propagateObjectSerializerIfNecessary(TcpProtocol protocol, ObjectSerializer objectSerializer)
    {
        if (protocol instanceof AbstractByteProtocol)
        {
            ((AbstractByteProtocol) protocol).setObjectSerializer(objectSerializer);
        }
    }
}
