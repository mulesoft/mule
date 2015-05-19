/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.processor.PostProcessAction;

import java.io.InputStream;

/**
 * An interface for common behavior of {@link InputStream}s
 * <p/>
 * that the SFTP transport uses, regardless of their base class.
 * It gathers functionality from {@link PostProcessAction} and
 * {@link ErrorOccurredDecorator} interfaces while adding additional behaviour.
 *
 * @since 3.7.0
 */
interface SftpStream extends PostProcessAction, ErrorOccurredDecorator
{

    /**
     * @return Whether the stream has been closed
     */
    boolean isClosed();
}
