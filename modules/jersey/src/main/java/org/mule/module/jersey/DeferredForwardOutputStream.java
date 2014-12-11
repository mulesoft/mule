/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static org.mule.util.Preconditions.checkState;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link java.io.OutputStream} that forwards its
 * write invokations to another stream. It's deferred because
 * it supports accepting write operations even before
 * the delegate stream has been provided, applying all
 * its contents once it becomes available. Any write operations
 * received after the delegate was provided are dispatched
 * directly
 *
 * @since 3.6.0
 */
public class DeferredForwardOutputStream extends OutputStream
{

    private class DeferredWrite
    {

        private final byte[] buffer;

        public DeferredWrite(byte[] buffer)
        {
            this.buffer = buffer;
        }

        void write(OutputStream outputStream) throws IOException
        {
            outputStream.write(buffer);
        }
    }

    private OutputStream delegate;
    private final List<DeferredWrite> deferredWrites = new LinkedList<>();
    private boolean closeRequested = false;

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        if (b == null)
        {
            throw new NullPointerException();
        }
        else if ((off < 0) || (off > b.length) || (len < 0) ||
                 ((off + len) > b.length) || ((off + len) < 0))
        {
            throw new IndexOutOfBoundsException();
        }
        else if (len == 0)
        {
            return;
        }

        synchronized (this)
        {
            if (delegate != null)
            {
                delegate.write(b, off, len);
            }
            else
            {
                byte[] buffer = new byte[len];
                System.arraycopy(b, off, buffer, 0, len);
                deferredWrites.add(new DeferredWrite(buffer));
            }
        }
    }

    @Override
    public synchronized void write(int b) throws IOException
    {
        if (delegate != null)
        {
            delegate.write(b);
        }
        else
        {
            deferredWrites.add(new DeferredWrite(new byte[] {(byte) b}));
        }
    }

    public synchronized void setDelegate(OutputStream delegate) throws IOException
    {
        checkState(this.delegate == null, "delegate can only be set once");
        this.delegate = delegate;

        try
        {
            for (DeferredWrite deferred : deferredWrites)
            {
                deferred.write(delegate);
            }

            if (closeRequested)
            {
                close();
            }
        }
        finally
        {
            deferredWrites.clear();
        }
    }

    @Override
    public synchronized void close() throws IOException
    {
        if (delegate != null)
        {
            delegate.close();
        }
        else
        {
            closeRequested = true;
        }
    }
}
