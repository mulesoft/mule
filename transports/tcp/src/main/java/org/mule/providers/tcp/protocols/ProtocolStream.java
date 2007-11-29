/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.providers.tcp.TcpProtocol;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.OutputStream;

public class ProtocolStream extends OutputStream
{

    private boolean streamOk;
    private TcpProtocol protocol;
    private OutputStream os;

    public ProtocolStream(TcpProtocol protocol, boolean streamOk, OutputStream os)
    {
        this.protocol = protocol;
        this.streamOk = streamOk;
        this.os = os;
    }

    private void assertStreamOk()
    {
        if (!streamOk)
        {
             throw new IllegalArgumentException("TCP protocol " + ClassUtils.getSimpleName(protocol.getClass()) +
                     " does not support streaming output");
        }
    }

    public void write(byte b[]) throws IOException
    {
        assertStreamOk();
        protocol.write(os, b);
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        assertStreamOk();
        byte[] buffer = new byte[len];
        System.arraycopy(b, off, buffer, 0, len);
        protocol.write(os, buffer);
    }

    public void flush() throws IOException
    {
        assertStreamOk();
        os.flush();
    }

    public void write(int b) throws IOException
    {
        write(new byte[]{(byte) b});
    }

    public void close() throws IOException
    {
        assertStreamOk();
        os.close();
    }

}
