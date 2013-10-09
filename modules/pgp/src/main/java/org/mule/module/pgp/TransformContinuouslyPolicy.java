/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import java.io.PipedOutputStream;

/**
 * A {@link TransformPolicy} that copies the transformed bytes continuously into the {@link PipedOutputStream}
 * without taking into account about how many bytes the object has requested.
 */
public class TransformContinuouslyPolicy extends AbstractTransformPolicy
{

    public static final long DEFAULT_CHUNK_SIZE = 1 << 24;
    
    private long chunkSize;

    public TransformContinuouslyPolicy()
    {
        this(DEFAULT_CHUNK_SIZE);
    }

    public TransformContinuouslyPolicy(long chunkSize)
    {
        this.chunkSize = chunkSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRequest(long length)
    {
        /**
         * Avoid calling super so that we don't add more bytes. 
         * The ContinuousWork will add the requested bytes as necessary
         * only start the copying thread
         */
        startCopyingThread();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Thread getCopyingThread()
    {
        return new ContinuousWork();
    }

    private class ContinuousWork extends TransformerWork
    {
        @Override
        protected void execute() throws Exception
        {
            getTransformer().initialize(getInputStream().getOut());
            
            boolean finishWriting = false;
            while (!finishWriting)
            {
                getBytesRequested().addAndGet(chunkSize);
                finishWriting = getTransformer().write(getInputStream().getOut(), getBytesRequested());
            }            
        }
    }
}


