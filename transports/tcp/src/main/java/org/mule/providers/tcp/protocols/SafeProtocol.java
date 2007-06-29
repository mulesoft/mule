/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.providers.tcp.TcpProtocol;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This precedes every message with a cookie and has a limited capactiy.
 * It should probably not be used in production.
 * We use ths protocol as the default because previously people tended to use DefaultProtocol
 * without considering packet fragmentation etc.
 * You should probably change to LengthProtocol.
 * Remember - both sender and receiver must use the same protocol.
 */
public class SafeProtocol implements TcpProtocol
{

    public static final int MAX_PAYLOAD_LENGTH = 1000000;
    public static final String OPEN_COOKIE = "You are using SafeProtocol";
    private TcpProtocol delegate = new LengthProtocol(MAX_PAYLOAD_LENGTH);

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

    private void assureSibling(OutputStream os) throws IOException
    {
        delegate.write(os, OPEN_COOKIE);
    }

    private boolean assertSiblingSafe(InputStream is) throws IOException
    {
        Object cookie = null;
        try
        {
            cookie = delegate.read(is);
        }
        catch (Exception e)
        {
            helpUser(e);
        }
        if (null != cookie)
        {
            if (!(cookie instanceof byte[]
                    && ((byte[]) cookie).length == OPEN_COOKIE.length()
                    && OPEN_COOKIE.equals(new String((byte[]) cookie))))
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
        throw (IOException) new IOException("An error occurred while verifying you connection.  "
                + "You may not be using a consistent protocol on your TCP transport. "
                + "Please read the documentation for the TCP transport, "
                + "paying particular attention to the protocol parameter.").initCause(e);
    }

}
