/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public Socket getSocket()
    {
        return socket;
    }

}
