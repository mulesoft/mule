/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import java.io.InputStream;

/**
 * An interface for common behavior of {@link InputStream}s
 * that the SFTP transport uses, regardless of their base class.
 *
 * @since 3.7.0
 */
interface SftpStream extends ErrorOccurredDecorator
{

    /**
     * @return Whether the stream has been closed
     */
    boolean isClosed();

    /**
     * Executes post processing actions like closing connections,
     * deleting/moving files, etc... The actual actions
     * to be executed depend on the implementation
     *
     * @throws Exception
     */
    void postProcess() throws Exception;

    /**
     * Specifies if {@link #postProcess()} should be executed when the stream is closed
     *
     * @param postProcessOnClose a {@link boolean}
     */
    void performPostProcessingOnClose(boolean postProcessOnClose);
}
