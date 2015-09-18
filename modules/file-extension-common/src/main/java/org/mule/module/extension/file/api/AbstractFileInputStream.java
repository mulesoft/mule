/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.AutoCloseInputStream;

/**
 * Base class for {@link InputStream} instances returned by connectors
 * which operate over a {@link FileSystem}.
 * <p>
 * It's an {@link AutoCloseInputStream} which also contains the concept
 * of a {@link PathLock} which is released when the stream is closed
 * or fully consumed.
 *
 * @since 4.0
 */
public abstract class AbstractFileInputStream extends AutoCloseInputStream
{

    private final PathLock lock;

    public AbstractFileInputStream(InputStream in, PathLock lock)
    {
        super(in);
        this.lock = lock;
    }

    /**
     * Closes the stream and invokes {@link PathLock#release()}
     * on the {@link #lock}
     *
     * @throws IOException in case of error
     */
    @Override
    public final synchronized void close() throws IOException
    {
        try
        {
            doClose();
        }
        finally
        {
            lock.release();
        }
    }

    protected void doClose() throws IOException
    {
        super.close();
    }
}
