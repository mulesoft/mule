/*
 * $Id: CloseCountDownInputStream.java 8077 2007-08-27 20:15:25Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.streaming;

import java.io.IOException;
import java.io.InputStream;

public class DelegatingInputStream extends InputStream
{

    private InputStream delegate;
    
    public DelegatingInputStream(InputStream delegate)
    {
        this.delegate = delegate;
    }

    public int available() throws IOException
    {
        return delegate.available();
    }

    public synchronized void mark(int readlimit)
    {
        delegate.mark(readlimit);
    }

    public boolean markSupported()
    {
        return delegate.markSupported();
    }

    public synchronized void reset() throws IOException
    {
        delegate.reset();
    }

    public long skip(long n) throws IOException
    {
        return delegate.skip(n);
    }

    public int read() throws IOException
    {
        return delegate.read();
    }

    public int read(byte b[]) throws IOException
    {
        return delegate.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException
    {
        return delegate.read(b, off, len);
    }

    public void close() throws IOException
    {
        delegate.close();
    }

}
