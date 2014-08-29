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

    private interface DeferredWrite
    {

        void write(OutputStream outputStream) throws IOException;
    }

    private class BufferedDeferredWrite implements DeferredWrite
    {

        private final byte[] buf;
        private final int off;
        private final int len;

        private BufferedDeferredWrite(byte[] buf, int off, int len)
        {
            this.buf = buf;
            this.off = off;
            this.len = len;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException
        {
            outputStream.write(buf, off, len);
        }
    }

    private class ByteDeferredWrite implements DeferredWrite
    {

        private final int b;

        private ByteDeferredWrite(int b)
        {
            this.b = b;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException
        {
            outputStream.write(b);
        }
    }

    private OutputStream delegate;
    private final List<DeferredWrite> deferredWrites = new LinkedList<>();

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
                deferredWrites.add(new BufferedDeferredWrite(b, off, len));
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
            deferredWrites.add(new ByteDeferredWrite(b));
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
        }
        finally
        {
            deferredWrites.clear();
        }
    }
}
