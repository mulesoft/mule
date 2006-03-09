/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * <code>ResponseOutputStream</code> is an output stream associated with the
 * currently recieved event. Note that if the stream is from a socket the socket
 * is also available on this stream so that the socket state can be validated
 * before writing.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ResponseOutputStream extends BufferedOutputStream
{
    private static ByteArrayOutputStream defaultStream = new ByteArrayOutputStream();

    private boolean used = false;
    private boolean isDefault = false;
    private Socket socket = null;

    public ResponseOutputStream()
    {
        super(defaultStream);
        isDefault = true;
    }

    public ResponseOutputStream(OutputStream stream)
    {
        super(stream);
    }

    public ResponseOutputStream(OutputStream stream, Socket socket)
    {
        super(stream);
        this.socket = socket;
    }

    public void write(int b) throws IOException
    {
        super.write(b);
        used = true;
    }

    public byte[] getBytes() throws IOException
    {
        if (isDefault) {
            flush();
            return defaultStream.toByteArray();
        }
        return null;
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
