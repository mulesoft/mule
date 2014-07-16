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
 * A {@link TransformPolicy} that copies only the requested transformed bytes
 * into the {@link PipedOutputStream}.
 */
public class TransformPerRequestPolicy extends AbstractTransformPolicy
{

    private Semaphore writeSemaphore;

    public TransformPerRequestPolicy()
    {
        this.writeSemaphore = new Semaphore(1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRequest(long length)
    {
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
                finishWriting = getTransformer().write(getInputStream().getOut(), getBytesRequested());
            }
        }
    }
}


