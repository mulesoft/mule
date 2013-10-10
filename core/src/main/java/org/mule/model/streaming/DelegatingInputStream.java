/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.streaming;

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
