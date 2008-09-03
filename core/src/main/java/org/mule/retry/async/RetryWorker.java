/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry.async;

import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryTemplate;
import org.mule.transport.FatalConnectException;
import org.mule.util.concurrent.Latch;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link javax.resource.spi.work.Work} implementation used when executing a {@link org.mule.api.retry.RetryTemplate}
 * in a separate thread
 */
public class RetryWorker implements Work
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(RetryWorker.class);

    private RetryCallback callback;
    private FatalConnectException exception = null;
    private FutureRetryContext context = new FutureRetryContext();
    private RetryTemplate delegate;
    private Latch startLatch;

    public RetryWorker(RetryTemplate delegate, RetryCallback callback)
    {
        this(delegate, callback, null);
    }

    public RetryWorker(RetryTemplate delegate, RetryCallback callback, Latch startLatch)
    {
        this.callback = callback;
        this.delegate = delegate;
        this.startLatch = startLatch;
        if (this.startLatch == null)
        {
            this.startLatch = new Latch();
            this.startLatch.countDown();
        }
    }

    public void release()
    {

    }

    public void run()
    {
        try
        {
            startLatch.await();
        }
        catch (InterruptedException e)
        {
            logger.warn("Retry thread interupted for callback: " + callback.getWorkDescription());
            return;
        }
        try
        {

            context.setDelegateContext(delegate.execute(callback));
        }
        catch (FatalConnectException e)
        {
            this.exception = e;
            logger.fatal(e, e);
            
        }
    }

    public FatalConnectException getException()
    {
        return exception;
    }

    public FutureRetryContext getRetryContext()
    {
        return context;
    }
}