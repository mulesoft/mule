/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import org.mule.module.extension.file.PathLock;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.io.input.ReaderInputStream;

/**
 * {@link InputStream} implementation used to obtain
 * a file's content based on a {@link Reader}.
 * <p>
 * This stream will automatically close itself once fully
 * consumed but will not fail if {@link #close()} is invoked
 * after that.
 * <p>
 * This class also contains a {@link PathLock} which will be
 * released when the stream is closed. However, this class
 * will never invoke the {@link PathLock#tryLock()} method
 * on it, it's the responsibility of whomever is creating
 * this instance to determine if that lock is to be acquired.
 *
 * @since 4.0
 */
final class FileInputStream extends AutoCloseInputStream
{

    private final PathLock lock;

    /**
     * Creates a new instance
     *
     * @param reader a {@link Reader}
     * @param lock   a {@link PathLock}
     */
    public FileInputStream(Reader reader, PathLock lock)
    {
        super(new ReaderInputStream(reader));
        this.lock = lock;
    }

    /**
     * Closes the stream and invokes {@link PathLock#release()}
     * on the {@link #lock}
     *
     * @throws IOException in case of error
     */
    @Override
    public void close() throws IOException
    {
        try
        {
            super.close();
        }
        finally
        {
            lock.release();
        }
    }
}
