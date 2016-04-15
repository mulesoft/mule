/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.commons.lang.Validate;

/**
 * A {@link LazyTransformedInputStream} represents an {@link InputStream} that
 * has been transformed when someone needs to read from it.
 * 
 * Internally, the {@link LazyTransformedInputStream} has a pipe that is written by an
 * {@link StreamTransformer} according to a {@link TransformPolicy}.
 * 
 * The {@link LazyTransformedInputStream} uses a separate thread for writing on the pipe
 * and delays it destruction till this {@link InputStream} is closed of finalized. In this way
 * we avoid any problems with broken pipes.
 */
public class LazyTransformedInputStream extends InputStream
{
    private PipedInputStream in;
    private PipedOutputStream out;
    private TransformPolicy transformPolicy;
    private StreamTransformer transformer;
    
    public LazyTransformedInputStream(TransformPolicy transformPolicy, StreamTransformer transformer) throws IOException
    {
        Validate.notNull(transformPolicy, "The transformPolicy should not be null");
        Validate.notNull(transformer, "The transformer should not be null");

        this.in = new PipedInputStream();
        this.out = new PipedOutputStream(this.in);
        this.transformPolicy = transformPolicy;
        this.transformer = transformer;
        this.transformPolicy.initialize(this);
    }

    @Override
    public int available() throws IOException
    {
        this.transformPolicy.readRequest(100);
        return this.in.available();
    }

    @Override
    public void close() throws IOException
    {
        this.in.close();
        this.transformPolicy.release();
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        this.transformPolicy.release();
    }
    
    @Override
    public synchronized void mark(int readlimit)
    {
        this.in.mark(readlimit);
    }

    @Override
    public boolean markSupported()
    {
        return this.in.markSupported();
    }

    @Override
    public int read() throws IOException
    {
        this.transformPolicy.readRequest(1);
        return this.in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        this.transformPolicy.readRequest(len);
        return this.in.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        this.transformPolicy.readRequest(b.length);
        return this.in.read(b);
    }

    @Override
    public synchronized void reset() throws IOException
    {
        this.in.reset();
    }

    @Override
    public long skip(long n) throws IOException
    {
        this.transformPolicy.readRequest(n);
        return this.in.skip(n);
    }

    PipedOutputStream getOut()
    {
        return out;
    }
    
    StreamTransformer getTransformer()
    {
        return transformer;
    }
}
