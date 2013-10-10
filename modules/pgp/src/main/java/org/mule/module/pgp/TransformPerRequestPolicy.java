/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


