/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import org.mule.ResponseOutputStream;
import org.mule.transport.tcp.TcpProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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

    public void write(OutputStream os, Object data) throws IOException
    {
        assureSibling(os);
        delegate.write(os, data);
    }

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

}
