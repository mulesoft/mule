/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
