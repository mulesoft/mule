/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * <code>ResponseOutputStream</code> is an output stream associated with the
 * currently received event. Note that if the stream is from a socket the socket is
 * also available on this stream so that the socket state can be validated before
 * writing.
 */

public class ResponseOutputStream extends BufferedOutputStream
{

    private boolean used = false;
    private Socket socket = null;

    public ResponseOutputStream(OutputStream stream)
    {
        super(stream);
    }

    public ResponseOutputStream(Socket socket, OutputStream stream) throws IOException
    {
        super(stream);
        this.socket = socket;
    }

    public void write(int b) throws IOException
    {
        super.write(b);
        used = true;
    }

    public synchronized void write(byte b[], int off, int len) throws IOException
    {
        super.write(b, off, len);
        used = true;
    }

    public void write(byte b[]) throws IOException
    {
        super.write(b);
        used = true;
    }

    public boolean isUsed()
    {
        return used;
    }

    public Socket getSocket()
    {
        return socket;
    }

}
