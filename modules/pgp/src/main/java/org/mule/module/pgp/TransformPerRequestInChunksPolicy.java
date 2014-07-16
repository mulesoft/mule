/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.PipedOutputStream;
import java.util.concurrent.Semaphore;

/**
 * A {@link TransformPolicy} that copies the requested transformed bytes in chunks
 * into the {@link PipedOutputStream}.
 */
public class TransformPerRequestInChunksPolicy extends AbstractTransformPolicy
{

    private Semaphore writeSemaphore;
    private long chunkSize;
    private long bytesActuallyRequested;

    public TransformPerRequestInChunksPolicy(long chunkSize)
    {
        this.writeSemaphore = new Semaphore(1);
        this.chunkSize = chunkSize;
        this.bytesActuallyRequested = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRequest(long length)
    {
        this.bytesActuallyRequested = this.bytesActuallyRequested + length;
        super.readRequest(length);
        this.writeSemaphore.release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        this.writeSemaphore.release();
        super.release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Thread getCopyingThread()
    {
        return new PerRequestWork();
    }

    private class PerRequestWork extends TransformerWork
    {
        @Override
        protected void execute() throws Exception
        {
            getTransformer().initialize(getInputStream().getOut());

            boolean finishWriting = false;
            while (!finishWriting && !isClosed)
            {
                writeSemaphore.acquire();

                /**
                 * Assuming one thread is reading the input stream (which is reasonable)
                 * and the state of the reading thread which should be delayed at this point
                 * it is safe to manipulate getBytesRequested() as I'm the only thread accessing the object
                 */
                long requested = bytesActuallyRequested;
                long updatedRequest = (long) (Math.ceil((double)requested / (double)chunkSize) * chunkSize);
                getBytesRequested().set(updatedRequest);

                finishWriting = getTransformer().write(getInputStream().getOut(), getBytesRequested());
            }
        }
    }
}
