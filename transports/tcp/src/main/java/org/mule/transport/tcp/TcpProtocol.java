/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.ResponseOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The TcpProtocol interface enables to plug different application level protocols on
 * a TcpConnector.  Note that this interface has lost the direct byte array write method.
 * Standard callers should (and will, since it matches the same signature, which is why
 * the method has not been deprecated) use the generic method instead..
 */
public interface TcpProtocol
{

    /**
     * Reads the input stream and returns a whole message.
     * 
     * @param is the input stream
     * @return an array of byte containing a full message
     * @throws IOException if an exception occurs
     */
    Object read(InputStream is) throws IOException;

    /**
     * Write the specified message to the output stream.
     * 
     * @param os the output stream to write to
     * @param data the data to write
     * @throws IOException if an exception occurs
     */
    void write(OutputStream os, Object data) throws IOException;

    /**
     * This lets protocols encode a response stream.  If the protocol does not support a
     * response stream (ie does not support streaming) then the stream should thrown an
     * exception when used.
     *
     * @param socket The destination to write to
     * @return A stream whose output will be encoded
     * @throws IOException
     */
    ResponseOutputStream createResponse(Socket socket) throws IOException;

}
