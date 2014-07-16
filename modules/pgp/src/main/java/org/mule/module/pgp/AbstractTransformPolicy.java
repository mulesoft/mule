/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract implementation of {@link TransformPolicy}.
 *
 * Subclasses must define the behavior of the copying {@link Thread}
 */
public abstract class AbstractTransformPolicy implements TransformPolicy
{
    protected static final Log logger = LogFactory.getLog(AbstractTransformPolicy.class);

    private AtomicBoolean startedCopying;
    private Thread copyingThread;
    private LazyTransformedInputStream inputStream;
    protected volatile boolean isClosed;
    private AtomicLong bytesRequested;

    public AbstractTransformPolicy()
    {
        this.startedCopying = new AtomicBoolean(false);
        this.isClosed = false;
        this.bytesRequested = new AtomicLong(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(LazyTransformedInputStream lazyTransformedInputStream) {
        this.inputStream = lazyTransformedInputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRequest(long length)
    {
        this.bytesRequested.addAndGet(length);
        startCopyingThread();
    }

    protected void startCopyingThread()
    {
        if (this.startedCopying.compareAndSet(false, true))
        {
            this.copyingThread = this.getCopyingThread();
            this.copyingThread.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        this.isClosed = true;
        if (this.copyingThread != null)
        {
            synchronized (this.copyingThread)
            {
                this.copyingThread.notifyAll();
            }
        }
    }

    /**
     * @return an instance of the copying {@link Thread}
     */
    protected abstract Thread getCopyingThread();

    protected StreamTransformer getTransformer()
    {
        return this.inputStream.getTransformer();
    }

    protected LazyTransformedInputStream getInputStream()
    {
        return this.inputStream;
    }

    protected AtomicLong getBytesRequested()
    {
        return bytesRequested;
    }

    protected abstract class TransformerWork extends Thread
    {
        @Override
        public synchronized void run()
        {
            try
            {
                execute();
                IOUtils.closeQuietly(getInputStream().getOut());
                // keep the thread alive so that we don't break the pipe
                while (!isClosed)
                {
                    try
                    {
                        this.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
                /**
                 * if an exception was thrown, the {@link PipedInputStream} may not even have a reference to this thread
                 * and wait forever. Therefore, we write the message and finish so we break the pipe.
                 */
                try
                {
                    IOUtils.write(e.getMessage().toCharArray(), getInputStream().getOut());
                }
                catch (IOException exp)
                {
                }
            }
        }

        protected abstract void execute() throws Exception;
    }
}
