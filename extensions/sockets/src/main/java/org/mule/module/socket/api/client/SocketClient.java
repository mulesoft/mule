/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.socket.api.client;

import org.mule.module.socket.api.exceptions.ReadingTimeoutException;
import org.mule.module.socket.api.socket.SocketProperties;
import org.mule.module.socket.api.source.SocketAttributes;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for common operations that can be done using a socket.
 *
 * @since 4.0
 */
public interface SocketClient
{

    /**
     * @param data to be written into the socket
     * @throws IOException
     */
    void write(Object data) throws IOException;

    /**
     * This methods blocks until new data is available or {@link SocketProperties#getClientTimeout()} is reached, in
     * which case a {@link ReadingTimeoutException} is thrown.
     * @return an {@link InputStream} with the information read from the socket.
     * @throws IOException
     */
    InputStream read() throws IOException;

    /**
     * Closes the connection that was held by the client, leaving it unusable.
     *
     * @throws IOException if an I/O error occurs when closing the socket
     */
    void close() throws IOException;

    /**
     * @return {@link SocketAttributes} with information associated to the client's connection.
     */
    SocketAttributes getAttributes();
}
