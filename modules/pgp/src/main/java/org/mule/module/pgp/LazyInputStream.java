/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;

/**
 * A {@link LazyInputStream} has a pipe that is written by an
 * {@link OutputStreamWriter} but it is delay till a client of this object
 * tries to read the stream.
 * 
 * The {@link LazyInputStream} uses a separate thread for writing on the pipe
 * and delays it destruction till this {@link InputStream} is closed. In this way
 * we avoid any problems with broken pipes.
 */
public class LazyInputStream extends InputStream
{

    private PipedInputStream in;
    private PipedOutputStream out;

    private AtomicBoolean startedCopying;
    private Thread copyingThread;
    private AtomicLong bytesRequested;

    private OutputStreamWriter writer;

    public LazyInputStream(OutputStreamWriter writer) throws IOException
    {
        this.in = new PipedInputStream();
        this.out = new PipedOutputStream(this.in);
        this.startedCopying = new AtomicBoolean(false);
        this.bytesRequested = new AtomicLong(0);
        this.writer = writer;
    }

    private void copyRequest()
    {
        if (this.startedCopying.compareAndSet(false, true))
        {
            this.copyingThread = new WriteWork();
            this.copyingThread.start();
        }
    }

    @Override
    public int available() throws IOException
    {
        this.copyRequest();
        return this.in.available();
    }

    @Override
    public void close() throws IOException
    {
        this.in.close();
        this.copyingThread.interrupt();
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
        this.bytesRequested.addAndGet(1);
        this.copyRequest();
        return this.in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        this.bytesRequested.addAndGet(len);
        this.copyRequest();
        return this.in.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        this.bytesRequested.addAndGet(b.length);
        this.copyRequest();
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
        this.copyRequest();
        return this.in.skip(n);
    }

    private class WriteWork extends Thread
    {
        public void run()
        {
            try
            {
                writer.initialize(out);
                
                boolean finishWriting = false;
                while (!finishWriting)
                {
                    finishWriting = writer.write(out, bytesRequested);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                IOUtils.closeQuietly(out);
                while (!this.isInterrupted())
                {
                    try
                    {
                        sleep(1000 * 60);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
        }
    }
}
